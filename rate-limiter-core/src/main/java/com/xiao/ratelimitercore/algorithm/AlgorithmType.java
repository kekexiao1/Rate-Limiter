package com.xiao.ratelimitercore.algorithm;

/**
 * 限流算法类型枚举
 */
public enum AlgorithmType {
    /**
     * 滑动窗口算法
     */
    SLIDING_WINDOW,
    
    /**
     * 令牌桶算法
     */
    TOKEN_BUCKET,

    /**
     * 本机限流算法
     */
    LOCAL

}