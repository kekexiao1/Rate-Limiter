package com.xiao.ratelimiterfallback;

import com.xiao.ratelimitercore.algorithm.RateLimitAlgorithm;
import com.xiao.ratelimiterfallback.algorithm.LocalRateLimitAlgorithm;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 降级容错自动配置类
 * 提供本地限流算法作为Redis不可用时的降级方案
 */
@AutoConfiguration
public class FallbackAutoConfiguration {

    /**
     * 本地限流算法Bean
     * 当Redis不可用时作为降级方案
     */
    @Bean
    @ConditionalOnMissingBean(name = "localRateLimitAlgorithm")
    public RateLimitAlgorithm localRateLimitAlgorithm() {
        return new LocalRateLimitAlgorithm();
    }
}
