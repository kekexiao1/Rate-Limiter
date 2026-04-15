package com.xiao.ratelimiterconfig.manager;

import com.xiao.ratelimitercore.model.RateLimitRule;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流规则管理器
 * 维护一个本地内存缓存（ConcurrentHashMap）保存最新的限流规则
 */
@Slf4j
public class RateLimitRuleManager {
    
    /**
     * 限流规则缓存
     * key: 资源标识
     * value: 限流规则
     */
    private final Map<String, RateLimitRule> ruleCache = new ConcurrentHashMap<>();
    
    /**
     * 添加或更新限流规则
     * 
     * @param key 资源标识
     * @param rule 限流规则
     */
    public void putRule(String key, RateLimitRule rule) {
        ruleCache.put(key, rule);
        log.info("添加/更新限流规则: key={}, rule={}", key, rule);
    }
    
    /**
     * 删除限流规则
     * 
     * @param key 资源标识
     */
    public void removeRule(String key) {
        RateLimitRule removed = ruleCache.remove(key);
        if (removed != null) {
            log.info("删除限流规则: key={}", key);
        }
    }
    
    /**
     * 获取限流规则
     * 
     * @param key 资源标识
     * @return 限流规则，如果不存在则返回null
     */
    public RateLimitRule getRule(String key) {
        return ruleCache.get(key);
    }
    
    /**
     * 获取所有限流规则
     * 
     * @return 所有限流规则的副本
     */
    public Map<String, RateLimitRule> getAllRules() {
        return new ConcurrentHashMap<>(ruleCache);
    }
    
    /**
     * 清空所有限流规则
     */
    public void clearAllRules() {
        ruleCache.clear();
        log.info("清空所有限流规则");
    }
    
    /**
     * 检查是否存在指定key的规则
     * 
     * @param key 资源标识
     * @return true表示存在，false表示不存在
     */
    public boolean containsRule(String key) {
        return ruleCache.containsKey(key);
    }
    
    /**
     * 获取规则数量
     * 
     * @return 规则数量
     */
    public int getRuleCount() {
        return ruleCache.size();
    }
}