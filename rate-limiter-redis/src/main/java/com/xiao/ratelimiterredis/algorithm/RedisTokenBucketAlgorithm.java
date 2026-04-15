package com.xiao.ratelimiterredis.algorithm;

import com.xiao.ratelimitercore.algorithm.RateLimitAlgorithm;
import com.xiao.ratelimitercore.model.RateLimitResult;
import com.xiao.ratelimitercore.model.RateLimitRule;
import com.xiao.ratelimiterredis.executor.RedisScriptExecutor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;
import java.util.List;

/**
 * 基于Redis的令牌桶限流算法实现
 * 实现 core 包的 RateLimitAlgorithm 接口
 */
public class RedisTokenBucketAlgorithm implements RateLimitAlgorithm {
    
    private final RedisScriptExecutor scriptExecutor;
    private final RedisScript<List> tokenBucketScript;
    
    public RedisTokenBucketAlgorithm(RedisScriptExecutor scriptExecutor) {
        this.scriptExecutor = scriptExecutor;
        this.tokenBucketScript = createTokenBucketScript();
    }
    
    @Override
    public RateLimitResult tryAcquire(String key, RateLimitRule rule) {
        String tokensKey = "rate_limit:" + key + "_tokens";
        String timestampKey = "rate_limit:" + key + "_timestamp";
        double refillRate = (double) rule.getLimit() / rule.getWindow(); // 每秒补充的令牌数
        
        // 执行Lua脚本
        List<Long> result = scriptExecutor.execute(
            tokenBucketScript,
            Arrays.asList(tokensKey, timestampKey),
             rule.getLimit(), refillRate, rule.getWindow()
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
    
    private RedisScript<List> createTokenBucketScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/token_bucket.lua"));
        script.setResultType(List.class);
        return script;
    }
}