package com.xiao.ratelimitercore.algorithm;

import com.xiao.ratelimitercore.model.RateLimitResult;
import com.xiao.ratelimitercore.model.RateLimitRule;

/**
 * 限流算法接口
 * 定义限流算法的顶级抽象
 * 纯粹的 Java 领域模型和接口抽象，绝对不能出现任何 Redis 或 Spring 的依赖
 */
public interface RateLimitAlgorithm {
    
    /**
     * 尝试获取一个许可
     * 
     * @param key 资源标识
     * @param rule 限流规则
     * @return 限流执行结果
     */
    RateLimitResult tryAcquire(String key, RateLimitRule rule);
}