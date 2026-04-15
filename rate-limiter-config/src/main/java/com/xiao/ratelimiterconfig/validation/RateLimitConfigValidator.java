package com.xiao.ratelimiterconfig.validation;

import com.alibaba.nacos.shaded.com.google.common.util.concurrent.RateLimiter;
import com.xiao.ratelimitercore.algorithm.AlgorithmType;
import com.xiao.ratelimiterconfig.config.RateLimitConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 限流配置验证器
 * 验证Nacos配置中的限流规则是否合法
 */
@Slf4j
public class RateLimitConfigValidator {
    
    /**
     * 验证限流配置
     * 
     * @param config 限流配置
     * @return 验证结果，包含错误信息列表
     */
    public ValidationResult validate(RateLimitConfig config) {
        List<String> errors = new ArrayList<>();
        
        if (config == null || config.getRateLimit() == null) {
            errors.add("限流配置为空");
            return new ValidationResult(false, errors);
        }
        
        RateLimitConfig.RateLimit rateLimit = config.getRateLimit();
        
        // 验证全局配置
        if (rateLimit.getGlobal() != null && rateLimit.getGlobal().isEnabled()) {
            validateGlobalConfig(rateLimit.getGlobal(), errors);
        }
        
        // 验证降级配置
        if (rateLimit.getFallback() != null && rateLimit.getFallback().isEnabled()) {
            validateFallbackConfig(rateLimit.getFallback(), errors);
        }
        
        // 验证具体规则
        if (rateLimit.getRules() != null) {
            validateRules(rateLimit.getRules(), errors);
        }
        
        // 验证配置之间的逻辑关系
        validateLogicalRelationships(rateLimit, errors);
        
        boolean isValid = errors.isEmpty();
        if (isValid) {
            log.info("限流配置验证通过");
        } else {
            log.warn("限流配置验证失败，发现 {} 个错误: {}", errors.size(), errors);
        }
        
        return new ValidationResult(isValid, errors);
    }
    
    /**
     * 验证全局配置
     */
    private void validateGlobalConfig(RateLimitConfig.GlobalConfig global, List<String> errors) {
        if (global.getLimit() <= 0) {
            errors.add("全局限流阈值必须大于0，当前值: " + global.getLimit());
        }
        
        if (global.getWindow() <= 0) {
            errors.add("全局限流时间窗口必须大于0，当前值: " + global.getWindow());
        }
        
        if (global.getAlgorithm() == null) {
            errors.add("全局限流算法不能为空");
        } else if (global.getAlgorithm() == AlgorithmType.LOCAL) {
            errors.add("全局限流算法不能使用LOCAL算法，请使用SLIDING_WINDOW或TOKEN_BUCKET");
        }
    }
    
    /**
     * 验证降级配置
     */
    private void validateFallbackConfig(RateLimitConfig.FallbackConfig fallback, List<String> errors) {
        if (fallback.getLimit() <= 0) {
            errors.add("降级限流阈值必须大于0，当前值: " + fallback.getLimit());
        }
        
        if (fallback.getWindow() <= 0) {
            errors.add("降级限流时间窗口必须大于0，当前值: " + fallback.getWindow());
        }
        
        if (fallback.getAlgorithm() == null) {
            errors.add("降级限流算法不能为空");
        }
    }
    
    /**
     * 验证具体规则
     */
    private void validateRules(List<RateLimitConfig.Rule> rules, List<String> errors) {
        if (rules.isEmpty()) {
            errors.add("具体规则列表不能为空");
            return;
        }
        
        for (int i = 0; i < rules.size(); i++) {
            RateLimitConfig.Rule rule = rules.get(i);
            validateRule(rule, i, errors);
        }
        
        // 检查重复的key
        long distinctKeyCount = rules.stream().map(RateLimitConfig.Rule::getKey).distinct().count();
        if (distinctKeyCount != rules.size()) {
            errors.add("存在重复的限流规则key");
        }
    }
    
    /**
     * 验证单个规则
     */
    private void validateRule(RateLimitConfig.Rule rule, int index, List<String> errors) {
        if (rule.getKey() == null || rule.getKey().trim().isEmpty()) {
            errors.add("规则[" + index + "]的key不能为空");
        } else if (rule.getKey().trim().length() > 100) {
            errors.add("规则[" + index + "]的key长度不能超过100字符");
        }
        
        if (rule.getLimit() <= 0) {
            errors.add("规则[" + index + "]的限流阈值必须大于0，当前值: " + rule.getLimit());
        }
        
        if (rule.getWindow() <= 0) {
            errors.add("规则[" + index + "]的时间窗口必须大于0，当前值: " + rule.getWindow());
        }
        
        if (rule.getAlgorithm() == null) {
            errors.add("规则[" + index + "]的算法不能为空");
        }
        
    }
    
    /**
     * 验证配置之间的逻辑关系
     */
    private void validateLogicalRelationships(RateLimitConfig.RateLimit rateLimit, List<String> errors) {
        // 验证降级配置应该比全局配置更宽松
        if (rateLimit.getGlobal() != null && rateLimit.getGlobal().isEnabled() &&
            rateLimit.getFallback() != null && rateLimit.getFallback().isEnabled()) {
            
            RateLimitConfig.GlobalConfig global = rateLimit.getGlobal();
            RateLimitConfig.FallbackConfig fallback = rateLimit.getFallback();
            
            // 降级阈值应该大于或等于全局阈值
            if (fallback.getLimit() < global.getLimit()) {
                errors.add("降级限流阈值(" + fallback.getLimit() + ")不能小于全局限流阈值(" + global.getLimit() + ")");
            }
            
            // 降级时间窗口应该小于或等于全局时间窗口
            if (fallback.getWindow() > global.getWindow()) {
                errors.add("降级时间窗口(" + fallback.getWindow() + ")不能大于全局时间窗口(" + global.getWindow() + ")");
            }
        }

    }


    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}