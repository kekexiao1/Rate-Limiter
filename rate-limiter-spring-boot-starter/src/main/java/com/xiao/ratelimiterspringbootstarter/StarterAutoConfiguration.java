package com.xiao.ratelimiterspringbootstarter;

import com.xiao.ratelimiterconfig.manager.RateLimitRuleManager;
import com.xiao.ratelimitercore.algorithm.RateLimitAlgorithm;
import com.xiao.ratelimiterredis.algorithm.RedisAlgorithmFactory;
import com.xiao.ratelimiterredis.monitor.RedisHealthMonitor;
import com.xiao.ratelimiterspringbootstarter.aspect.RateLimiterAspect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 限流starter自动配置类
 * 集成所有限流模块的自动配置
 */
@AutoConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class StarterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterAspect rateLimiterAspect(RateLimitRuleManager ruleManager,
                                               RedisAlgorithmFactory factory,
                                               @Qualifier("localRateLimitAlgorithm") RateLimitAlgorithm localRateLimitAlgorithm,
                                               RedisHealthMonitor redisHealthMonitor){
        return new RateLimiterAspect(ruleManager, factory, localRateLimitAlgorithm,redisHealthMonitor);
    }
}
