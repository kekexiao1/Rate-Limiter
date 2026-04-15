package com.xiao.ratelimitercore.exception;

public class RedisUnavailableException extends RuntimeException {
    
    private final String operation;
    
    public RedisUnavailableException(String operation, Throwable cause) {
        super("Redis不可用，操作失败: " + operation, cause);
        this.operation = operation;
    }
    
    public RedisUnavailableException(String operation, String message) {
        super("Redis不可用: " + operation + ", 原因: " + message);
        this.operation = operation;
    }
    
    public String getOperation() {
        return operation;
    }
}
