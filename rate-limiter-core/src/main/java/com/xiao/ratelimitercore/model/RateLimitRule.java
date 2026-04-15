package com.xiao.ratelimitercore.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xiao.ratelimitercore.algorithm.AlgorithmType;

/**
 * 限流规则
 * 限流规则的载体（对应 Nacos 里配置的 YAML 映射过来的对象）
 */
public class RateLimitRule {
    
    private final String key;
    private final int limit;
    private final int window;
    private final AlgorithmType algorithmType;
    
    @JsonCreator
    public RateLimitRule(@JsonProperty("key") String key, 
                        @JsonProperty("limit") int limit, 
                        @JsonProperty("window") int window, 
                        @JsonProperty("algorithmType") AlgorithmType algorithmType) {
        this.key = key;
        this.limit = limit;
        this.window = window;
        this.algorithmType = algorithmType;
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
     * 获取时间窗口（秒）
     * 
     * @return 时间窗口秒数
     */
    public int getWindow() {
        return window;
    }
    
    /**
     * 获取算法类型
     * 
     * @return 算法类型枚举
     */
    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        RateLimitRule that = (RateLimitRule) o;
        
        if (limit != that.limit) return false;
        if (window != that.window) return false;
        if (!key.equals(that.key)) return false;
        return algorithmType == that.algorithmType;
    }
    
    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + limit;
        result = 31 * result + window;
        result = 31 * result + algorithmType.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return "RateLimitRule{" +
                "key='" + key + '\'' +
                ", limit=" + limit +
                ", window=" + window +
                ", algorithmType=" + algorithmType +
                '}';
    }
}