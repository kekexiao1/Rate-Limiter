package com.xiao.ratelimiterconfig.listener;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.xiao.ratelimiterconfig.config.NacosProperties;
import com.xiao.ratelimiterconfig.config.RateLimitConfig;
import com.xiao.ratelimitercore.model.RateLimitRule;
import com.xiao.ratelimiterconfig.event.RateLimitRuleChangeEvent;
import com.xiao.ratelimiterconfig.manager.RateLimitRuleManager;
import com.xiao.ratelimiterconfig.validation.RateLimitConfigValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Nacos配置监听器
 * 监听Nacos配置变更，维护本地内存缓存
 */
@Slf4j
@RequiredArgsConstructor
public class NacosConfigListener {

    private final NacosConfigManager nacosConfigManager;

    private final RateLimitRuleManager ruleManager;

    private final ApplicationEventPublisher eventPublisher;

    private final RateLimitConfigValidator rateLimitConfigValidator;

    private final NacosProperties nacosProperties;
    
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private final ThreadPoolExecutor asyncExecutor = new ThreadPoolExecutor(1,1,0L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(10)
                        , new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * 初始化监听器
     */
    @PostConstruct
    public void init() {
        try {
            // 首次加载配置
            loadInitialConfig();
            
            // 添加配置变更监听器
            addConfigListener();
            
            log.info("Nacos配置监听器初始化完成，监听dataId: {}, group: {}", nacosProperties.getDataId(), nacosProperties.getGroup());
        } catch (Exception e) {
            log.error("Nacos配置监听器初始化失败", e);
        }
    }
    
    /**
     * 首次加载配置
     */
    private void loadInitialConfig() throws Exception {
        String configContent = nacosConfigManager.getConfigService()
                .getConfig(nacosProperties.getDataId(), nacosProperties.getGroup(), 5000);
        
        if (configContent != null && !configContent.trim().isEmpty()) {
            Map<String, RateLimitRule> rules = parseConfigContent(configContent);
            updateRules(rules, RateLimitRuleChangeEvent.ChangeType.FULL_UPDATE);
            log.info("首次加载配置成功，共加载 {} 条规则", rules.size());
        } else {
            log.warn("首次加载配置为空，dataId: {}, group: {}", nacosProperties.getDataId(), nacosProperties.getGroup());
        }
    }
    
    /**
     * 添加配置变更监听器
     */
    private void addConfigListener() throws NacosException {
        nacosConfigManager.getConfigService().addListener(nacosProperties.getDataId(), nacosProperties.getGroup(), new Listener() {
            @Override
            public Executor getExecutor() {

                return asyncExecutor; // 使用默认执行器
            }
            
            @Override
            public void receiveConfigInfo(String configInfo) {
                try {
                    log.info("接收到Nacos配置变更通知");
                    handleConfigChange(configInfo);
                } catch (Exception e) {
                    log.error("处理Nacos配置变更失败", e);
                }
            }
        });
    }
    
    /**
     * 处理配置变更
     */
    private void handleConfigChange(String configContent) throws Exception {
        if (configContent == null || configContent.trim().isEmpty()) {
            // 配置被删除，清空所有规则
            ruleManager.clearAllRules();
            eventPublisher.publishEvent(new RateLimitRuleChangeEvent(this, 
                    ruleManager.getAllRules(), RateLimitRuleChangeEvent.ChangeType.FULL_UPDATE));
            log.info("配置被删除，已清空所有限流规则");
            return;
        }
        
        Map<String, RateLimitRule> newRules = parseConfigContent(configContent);
        Map<String, RateLimitRule> oldRules = ruleManager.getAllRules();
        
        // 比较新旧配置差异
        if (isFullUpdateNeeded(oldRules, newRules)) {
            updateRules(newRules, RateLimitRuleChangeEvent.ChangeType.FULL_UPDATE);
        } else {
            // 增量更新
            incrementalUpdate(oldRules, newRules);
        }
    }
    
    /**
     * 解析配置内容（YAML格式）
     */
    private Map<String, RateLimitRule> parseConfigContent(String configContent) throws Exception {
        RateLimitConfig config = yamlMapper.readValue(configContent, RateLimitConfig.class);
        Map<String, RateLimitRule> rules = new HashMap<>();
        
        if (config != null && config.getRateLimit() != null) {
            // 验证配置
            RateLimitConfigValidator.ValidationResult validationResult = rateLimitConfigValidator.validate(config);
            
            if (!validationResult.isValid()) {
                log.error("限流配置验证失败，拒绝应用配置。错误信息: {}", validationResult.getErrorMessage());
                throw new IllegalArgumentException("限流配置验证失败: " + validationResult.getErrorMessage());
            }

            RateLimitConfig.RateLimit rateLimit = config.getRateLimit();
            
            // 检查是否启用限流
            if (!rateLimit.isEnabled()) {
                log.info("限流功能已禁用，跳过规则解析");
                return rules;
            }
            
            // 解析全局配置
            if (rateLimit.getGlobal() != null && rateLimit.getGlobal().isEnabled()) {
                RateLimitConfig.GlobalConfig global = rateLimit.getGlobal();
                RateLimitRule globalRule = new RateLimitRule(
                        "global",
                        global.getLimit(),
                        global.getWindow(),
                        global.getAlgorithm()
                );
                rules.put("global", globalRule);
                log.debug("解析全局限流规则: limit={}, window={}, algorithm={}", 
                        global.getLimit(), global.getWindow(), global.getAlgorithm());
            }
            
            // 解析降级配置
            if (rateLimit.getFallback() != null && rateLimit.getFallback().isEnabled()) {
                RateLimitConfig.FallbackConfig fallback = rateLimit.getFallback();
                RateLimitRule fallbackRule = new RateLimitRule(
                        "fallback",
                        fallback.getLimit(),
                        fallback.getWindow(),
                        fallback.getAlgorithm()
                );
                rules.put("fallback", fallbackRule);
                log.debug("解析降级限流规则: limit={}, window={}, algorithm={}", 
                        fallback.getLimit(), fallback.getWindow(), fallback.getAlgorithm());
            }
            
            // 解析具体规则
            if (rateLimit.getRules() != null) {
                for (RateLimitConfig.Rule rule : rateLimit.getRules()) {
                    RateLimitRule rateLimitRule = new RateLimitRule(
                            rule.getKey(),
                            rule.getLimit(),
                            rule.getWindow(),
                            rule.getAlgorithm()
                    );
                    rules.put(rule.getKey(), rateLimitRule);
                    log.debug("解析自定义限流规则: key={}, limit={}, window={}, algorithm={}", 
                            rule.getKey(), rule.getLimit(), rule.getWindow(), rule.getAlgorithm());
                }
            }
        }
        
        log.info("配置解析完成，共解析 {} 条规则", rules.size());
        return rules;
    }
    
    /**
     * 判断是否需要全量更新
     */
    private boolean isFullUpdateNeeded(Map<String, RateLimitRule> oldRules, Map<String, RateLimitRule> newRules) {
        // 如果规则数量变化较大，或者有大量规则被删除，则进行全量更新
        int sizeDiff = Math.abs(oldRules.size() - newRules.size());
        return sizeDiff > oldRules.size() * 0.3; // 超过30%的变化
    }
    
    /**
     * 全量更新规则
     */
    private void updateRules(Map<String, RateLimitRule> rules, RateLimitRuleChangeEvent.ChangeType changeType) {
        ruleManager.clearAllRules();
        rules.forEach(ruleManager::putRule);
        eventPublisher.publishEvent(new RateLimitRuleChangeEvent(this, rules, changeType));
        log.info("全量更新规则完成，共更新 {} 条规则", rules.size());
    }
    
    /**
     * 增量更新规则
     */
    private void incrementalUpdate(Map<String, RateLimitRule> oldRules, Map<String, RateLimitRule> newRules) {
        // 处理新增和更新的规则
        newRules.forEach((key, newRule) -> {
            RateLimitRule oldRule = oldRules.get(key);
            if (oldRule == null) {
                // 新增规则
                ruleManager.putRule(key, newRule);
                eventPublisher.publishEvent(new RateLimitRuleChangeEvent(this, 
                        Map.of(key, newRule), RateLimitRuleChangeEvent.ChangeType.ADD, key));
                log.info("新增限流规则: key={}", key);
            } else if (!oldRule.equals(newRule)) {
                // 更新规则
                ruleManager.putRule(key, newRule);
                eventPublisher.publishEvent(new RateLimitRuleChangeEvent(this, 
                        Map.of(key, newRule), RateLimitRuleChangeEvent.ChangeType.UPDATE, key));
                log.info("更新限流规则: key={}", key);
            }
        });
        
        // 处理删除的规则
        oldRules.keySet().stream()
                .filter(key -> !newRules.containsKey(key))
                .forEach(key -> {
                    ruleManager.removeRule(key);
                    eventPublisher.publishEvent(new RateLimitRuleChangeEvent(this, 
                            Map.of(), RateLimitRuleChangeEvent.ChangeType.DELETE, key));
                    log.info("删除限流规则: key={}", key);
                });
    }
}