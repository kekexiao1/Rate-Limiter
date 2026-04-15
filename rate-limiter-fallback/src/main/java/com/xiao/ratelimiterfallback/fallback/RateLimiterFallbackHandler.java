package com.xiao.ratelimiterfallback.fallback;

import com.xiao.ratelimitercore.exception.RateLimitException;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 限流降级处理器接口
 * 当请求被限流时，可通过实现此接口来自定义降级策略
 */
public interface RateLimiterFallbackHandler {
    
    /**
     * 处理限流降级逻辑
     * @param joinPoint 切入点
     * @param exception 限流异常
     * @return 降级处理结果
     * @throws Throwable 异常
     */
    Object handle(ProceedingJoinPoint joinPoint, RateLimitException exception) throws Throwable;
}