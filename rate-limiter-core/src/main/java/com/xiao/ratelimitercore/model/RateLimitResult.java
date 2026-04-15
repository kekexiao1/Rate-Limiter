package com.xiao.ratelimitercore.model;

/**
 * 限流执行结果
 * 封装限流执行的结果
 */
public class RateLimitResult {
    
    private final boolean allowed;
    private final long remaining;
    private final long waitMs;
    
    private RateLimitResult(boolean allowed, long remaining, long waitMs) {
        this.allowed = allowed;
        this.remaining = remaining;
        this.waitMs = waitMs;
    }
    
    /**
     * 创建放行结果
     * 
     * @param remaining 剩余配额
     * @return 限流结果
     */
    public static RateLimitResult allowed(long remaining) {
        return new RateLimitResult(true, remaining, 0);
    }
    
    /**
     * 创建放行结果（带等待时间）
     * 
     * @param remaining 剩余配额
     * @param waitMs 预计需要等待的毫秒数
     * @return 限流结果
     */
    public static RateLimitResult allowed(long remaining, long waitMs) {
        return new RateLimitResult(true, remaining, waitMs);
    }
    
    /**
     * 创建拒绝结果
     * 
     * @param waitMs 预计需要等待的毫秒数
     * @return 限流结果
     */
    public static RateLimitResult rejected(long waitMs) {
        return new RateLimitResult(false, 0, waitMs);
    }
    
    /**
     * 是否放行
     * 
     * @return true表示放行，false表示拒绝
     */
    public boolean isAllowed() {
        return allowed;
    }
    
    /**
     * 获取剩余配额
     * 
     * @return 剩余配额数量
     */
    public long getRemaining() {
        return remaining;
    }
    
    /**
     * 获取预计等待时间
     * 
     * @return 预计需要等待的毫秒数（可用于 HTTP 响应头 X-RateLimit-Retry-After）
     */
    public long getWaitMs() {
        return waitMs;
    }
}