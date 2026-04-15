package com.xiao.ratelimiterfallback.fallback;

import com.xiao.ratelimitercore.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 默认限流降级处理器
 * 当请求被限流时，默认抛出限流异常
 */
@Slf4j
public class DefaultFallbackHandler implements RateLimiterFallbackHandler {
    
    @Override
    public Object handle(ProceedingJoinPoint joinPoint, RateLimitException exception) throws Throwable {
        log.warn("请求被限流，执行默认降级策略: {}", exception.getMessage());
        throw exception;
    }
}