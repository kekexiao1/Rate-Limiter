package com.xiao.ratelimiterredis.executor;

import com.xiao.ratelimitercore.exception.RateLimitException;
import com.xiao.ratelimitercore.exception.RedisUnavailableException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

/**
 * Redis脚本执行器
 * 屏蔽 Spring Data Redis 调用 Lua 脚本的底层模板代码
 */
@Slf4j
public class RedisScriptExecutor {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public RedisScriptExecutor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 执行Redis脚本
     * 
     * @param script Lua脚本
     * @param keys 键列表
     * @param args 参数列表
     * @param <T> 返回值类型
     * @return 脚本执行结果
     * @throws RedisUnavailableException 当Redis不可用时抛出
     */
    public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        try {
            return redisTemplate.execute(script, keys, args);
        } catch (RedisUnavailableException e) {
           throw new RedisUnavailableException("redis不可用", e);
        }
    }

}