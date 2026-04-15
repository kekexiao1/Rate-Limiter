package com.xiao.ratelimiterconfig;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.xiao.ratelimiterconfig.config.NacosProperties;
import com.xiao.ratelimiterconfig.listener.NacosConfigListener;
import com.xiao.ratelimiterconfig.manager.RateLimitRuleManager;
import com.xiao.ratelimiterconfig.validation.RateLimitConfigValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * 配置模块自动配置类
 * 当存在Nacos配置管理器时自动启用
 */
@AutoConfiguration
@ConditionalOnClass(NacosConfigService.class)
@ConditionalOnProperty(prefix = "rate-limiter", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(NacosProperties.class)
public class ConfigAutoConfiguration {

    /**
     * 限流规则管理器Bean
     */
    @Bean
    public RateLimitRuleManager rateLimitRuleManager() {
        return new RateLimitRuleManager();
    }

    @Bean
    public RateLimitConfigValidator rateLimitConfigValidator(){
        return new RateLimitConfigValidator();
    }
    
    /**
     * Nacos配置监听器Bean
     */
    @Bean(initMethod = "init")
    public NacosConfigListener nacosConfigListener(
            NacosConfigManager nacosConfigManager,
            RateLimitRuleManager rateLimitRuleManager,
            ApplicationEventPublisher applicationEventPublisher,
            RateLimitConfigValidator rateLimitConfigValidator,
            NacosProperties nacosProperties) {
        
        NacosConfigListener listener = new NacosConfigListener(nacosConfigManager, rateLimitRuleManager,
                applicationEventPublisher, rateLimitConfigValidator, nacosProperties);
        return listener;
    }
}
