package com.xiao.ratelimitercore.exception;

/**
 * 限流异常
 * 当限流被触发时，由 AOP 层抛出的自定义运行时异常
 */
public class RateLimitException extends RuntimeException {
    
    private final String key;
    private final int limit;
    private final int currentWindow;
    private final long waitMs;
    
    public RateLimitException(String key, int limit, int currentWindow, long waitMs, String message) {
        super(message);
        this.key = key;
        this.limit = limit;
        this.currentWindow = currentWindow;
        this.waitMs = waitMs;
    }
    
    /**
     * 获取资源标识
     * 
     * @return 资源标识
     */
    public String getKey() {
        return key;
    }
    
    /**
     * 获取限流阈值
     * 
     * @return 阈值
     */
    public int getLimit() {
        return limit;
    }
    
    /**
     * 获取当前窗口
     * 
     * @return 当前窗口
     */
    public int getCurrentWindow() {
        return currentWindow;
    }
    
    /**
     * 获取预计等待时间
     * 
     * @return 预计需要等待的毫秒数
     */
    public long getWaitMs() {
        return waitMs;
    }
}