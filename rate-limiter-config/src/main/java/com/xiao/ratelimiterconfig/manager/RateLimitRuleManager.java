package com.xiao.ratelimiterconfig.manager;

import com.xiao.ratelimitercore.model.RateLimitRule;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 限流规则管理器
 * 维护一个本地内存缓存（ConcurrentHashMap）保存最新的限流规则
 */
@Slf4j
public class RateLimitRuleManager {

    private final Map<String, RateLimitRule> ruleCache = new ConcurrentHashMap<>();


    public void putRule(String key, RateLimitRule rule) {
        ruleCache.put(key, rule);
        log.info("添加/更新限流规则: key={}, rule={}", key, rule);
    }

    public void removeRule(String key) {
        RateLimitRule removed = ruleCache.remove(key);
        if (removed != null) {
            log.info("删除限流规则: key={}", key);
        }
    }

    public RateLimitRule getRule(String key) {
        RateLimitRule rule = ruleCache.get(key);
        return rule != null ? rule : null;
    }

    public Map<String, RateLimitRule> getAllRules() {
        Map<String, RateLimitRule> result = new ConcurrentHashMap<>();
        ruleCache.forEach((k, v) -> result.put(k, v));
        return result;
    }

    public void clearAllRules() {
        ruleCache.clear();
        log.info("清空所有限流规则");
    }

    public boolean containsRule(String key) {
        return ruleCache.containsKey(key);
    }


}