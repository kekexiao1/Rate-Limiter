package com.xiao.ratelimiterfallback.algorithm;

import com.xiao.ratelimitercore.algorithm.RateLimitAlgorithm;
import com.xiao.ratelimitercore.model.RateLimitResult;
import com.xiao.ratelimitercore.model.RateLimitRule;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地限流算法（降级方案）
 * 当Redis不可用时，使用本地内存进行限流
 */
@Slf4j
public class LocalRateLimitAlgorithm implements RateLimitAlgorithm {

    private final ConcurrentHashMap<String, WindowData> windowCache = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult tryAcquire(String key, RateLimitRule rule) {
        long currentTime = System.currentTimeMillis();
        long windowMillis = rule.getWindow() * 1000L;
        
        WindowData windowData = windowCache.compute(key, (k, existing) -> {
            if (existing == null || currentTime - existing.startTime > windowMillis) {
                // 新窗口或窗口已过期
                return new WindowData(currentTime, new AtomicLong(0));
            }
            //窗口有效 → 直接复用
            return existing;
        });
        
        // 检查是否超限
        long currentCount = windowData.counter.incrementAndGet();
        
        if (currentCount <= rule.getLimit()) {
            long remaining = rule.getLimit() - currentCount;
            log.debug("本地限流检查通过: key={}, count={}, remaining={}", key, currentCount, remaining);
            return RateLimitResult.allowed(remaining);
        } else {
            log.warn("本地限流检查拒绝: key={}, count={}, limit={}", key, currentCount, rule.getLimit());
            return RateLimitResult.rejected(windowMillis);
        }
    }
    
    /**
     * 窗口数据
     */
    private static class WindowData {
        final long startTime;
        final AtomicLong counter;
        
        WindowData(long startTime, AtomicLong counter) {
            this.startTime = startTime;
            this.counter = counter;
        }
    }
}