package com.xiao.ratelimiterredis.algorithm;

import com.xiao.ratelimitercore.algorithm.RateLimitAlgorithm;
import com.xiao.ratelimitercore.model.RateLimitResult;
import com.xiao.ratelimitercore.model.RateLimitRule;
import com.xiao.ratelimiterredis.executor.RedisScriptExecutor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 基于Redis的滑动窗口限流算法实现
 * 实现 core 包的 RateLimitAlgorithm 接口
 */
public class RedisSlidingWindowAlgorithm implements RateLimitAlgorithm {
    
    private final RedisScriptExecutor scriptExecutor;
    private final RedisScript<List> slidingWindowScript;
    
    public RedisSlidingWindowAlgorithm(RedisScriptExecutor scriptExecutor) {
        this.scriptExecutor = scriptExecutor;
        this.slidingWindowScript = createSlidingWindowScript();
    }
    
    @Override
    public RateLimitResult tryAcquire(String key, RateLimitRule rule) {
        String redisKey = "rate_limit:" + key;
        long windowMillis = rule.getWindow() * 1000L;
        String requestId = UUID.randomUUID().toString();
        
        // 执行Lua脚本
        List<Long> result = scriptExecutor.execute(
            slidingWindowScript,
            Collections.singletonList(redisKey),
             windowMillis, rule.getLimit(), requestId
        );
        
        if (result == null || result.size() < 3) {
            return RateLimitResult.rejected(0);
        }
        
        long allowed = result.get(0);
        long remaining = result.get(1);
        long waitMs = result.get(2);
        
        if (allowed == 1) {
            return RateLimitResult.allowed(remaining, waitMs);
        } else {
            return RateLimitResult.rejected(waitMs);
        }
    }
    
    private RedisScript<List> createSlidingWindowScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/sliding_window.lua"));
        script.setResultType(List.class);
        return script;
    }
}