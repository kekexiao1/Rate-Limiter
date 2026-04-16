package com.xiao.ratelimiterfallback.algorithm;

import com.xiao.ratelimitercore.algorithm.RateLimitAlgorithm;
import com.xiao.ratelimitercore.model.RateLimitResult;
import com.xiao.ratelimitercore.model.RateLimitRule;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class LocalRateLimitAlgorithm implements RateLimitAlgorithm {

    private static final int BUCKET_COUNT = 100;
    private final ConcurrentHashMap<String, SlidingWindow> windowCache = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult tryAcquire(String key, RateLimitRule rule) {
        long currentTime = System.currentTimeMillis();
        long windowMillis = rule.getWindow() * 1000L;
        long bucketSize = windowMillis / BUCKET_COUNT;

        SlidingWindow window = windowCache.compute(key, (k, existing) -> {
            if (existing == null || currentTime - existing.startTime > windowMillis) {
                // 如果窗口过期了，直接创建一个全新的窗口
                return new SlidingWindow(currentTime);
            }
            return existing;
        });
        // 注意：这里需要加锁，因为计算索引、清理旧桶、求和必须是原子的
        return window.tryAcquire(currentTime, bucketSize, rule.getLimit(), windowMillis);
    }

    private static class SlidingWindow {
        private long startTime;
        private final AtomicLong[] buckets;
        private final ReentrantLock lock = new ReentrantLock();
        // 记录上一次更新到的桶索引，用于判断是否需要清理中间的桶
        private int lastUpdatedBucketIndex = -1;

        SlidingWindow(long startTime) {
            this.startTime = startTime;
            this.buckets = new AtomicLong[BUCKET_COUNT];
            for (int i = 0; i < BUCKET_COUNT; i++) {
                buckets[i] = new AtomicLong(0);
            }
        }

        public RateLimitResult tryAcquire(long currentTime, long bucketSize, long limit, long windowMillis) {
            lock.lock();
            try {
                // 1. 计算当前应该落在哪个小桶里
                int currentBucketIndex = (int) ((currentTime - startTime) / bucketSize) % BUCKET_COUNT;

                // 2. 防御长时间无请求导致的“幽灵数据”：如果跨度超过了一个完整窗口，全部清零
                if (currentTime - startTime >= windowMillis) {
                    for (AtomicLong bucket : buckets) {
                        bucket.set(0);
                    }
                    lastUpdatedBucketIndex = -1; // 重置游标
                }

                // 如果当前桶的索引和上一次的索引不同，说明时间往前走了，需要把中间跨过的旧桶清零
                if (currentBucketIndex != lastUpdatedBucketIndex) {
                    if (lastUpdatedBucketIndex == -1) {
                        // 第一次请求，不需要清零
                    } else if (currentBucketIndex > lastUpdatedBucketIndex) {
                        // 正常情况：比如上次是5，这次是8，把6和7清零
                        for (int i = lastUpdatedBucketIndex + 1; i <= currentBucketIndex; i++) {
                            buckets[i].set(0);
                        }
                    } else {
                        // 环绕情况：比如上次是98，这次是2，把99和0,1清零
                        for (int i = lastUpdatedBucketIndex + 1; i < BUCKET_COUNT; i++) {
                            buckets[i].set(0);
                        }
                        for (int i = 0; i <= currentBucketIndex; i++) {
                            buckets[i].set(0);
                        }
                    }
                    lastUpdatedBucketIndex = currentBucketIndex;
                }

                // 4. 当前桶请求 +1
                long count = buckets[currentBucketIndex].incrementAndGet();

                // 5. 计算整个大窗口的总请求量
                // 因为过期桶已经被清零了，所以现在直接把 100 个桶加起来就是准确的滑动窗口总数！
                long totalCount = 0;
                for (AtomicLong bucket : buckets) {
                    totalCount += bucket.get();
                }

                // 6. 判断限流
                if (totalCount <= limit) {
                    long remaining = limit - totalCount;
                    return RateLimitResult.allowed(remaining);
                } else {
                    // 拒绝时，回滚刚才 +1 的操作
                    buckets[currentBucketIndex].decrementAndGet();
                    long waitMs = bucketSize - (currentTime - startTime) % bucketSize;
                    return RateLimitResult.rejected(waitMs);
                }
            } finally {
                lock.unlock();
            }
        }
    }
}