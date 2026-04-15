package com.xiao.ratelimiterredis.listener;

import com.xiao.ratelimiterconfig.event.RateLimitRuleChangeEvent;
import com.xiao.ratelimitercore.algorithm.AlgorithmType;
import com.xiao.ratelimitercore.model.RateLimitRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redis规则变更事件监听器
 * 监听限流规则变更事件，自动更新Redis中的相关数据结构
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRuleChangeEventListener {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 处理限流规则变更事件
     * 
     * @param event 限流规则变更事件
     */
    @EventListener
    public void handleRateLimitRuleChange(RateLimitRuleChangeEvent event) {
        try {
            switch (event.getChangeType()) {
                case FULL_UPDATE:
                    handleFullUpdate(event.getRules());
                    break;
                case ADD:
                case UPDATE:
                    handleSingleRuleUpdate(event.getChangedKey(), event.getRules().get(event.getChangedKey()));
                    break;
                case DELETE:
                    handleSingleRuleDelete(event.getChangedKey());
                    break;
                default:
                    log.warn("未知的变更类型: {}", event.getChangeType());
                    break;
            }
            log.info("Redis限流规则更新完成，变更类型: {}, 变更资源: {}", 
                    event.getChangeType(), event.getChangedKey());
        } catch (Exception e) {
            log.error("处理Redis限流规则变更事件失败", e);
        }
    }

    /**
     * 处理全量更新
     * 
     * @param rules 新的规则集合
     */
    private void handleFullUpdate(Map<String, RateLimitRule> rules) {
        // 先清理所有现有的限流相关key
        clearAllRateLimitKeys();
        
        // 然后重新初始化所有规则
        rules.forEach(this::initializeRule);
        
        log.info("Redis全量更新完成，共处理 {} 条规则", rules.size());
    }

    /**
     * 处理单条规则更新
     * 
     * @param key 资源标识
     * @param rule 限流规则
     */
    private void handleSingleRuleUpdate(String key, RateLimitRule rule) {
        if (rule == null) {
            log.warn("规则为空，跳过更新: {}", key);
            return;
        }
        
        // 清理该规则相关的所有Redis key
        clearRuleKeys(key);
        
        // 重新初始化该规则
        initializeRule(key, rule);
        
        log.info("Redis单条规则更新完成: key={}", key);
    }

    /**
     * 处理单条规则删除
     * 
     * @param key 资源标识
     */
    private void handleSingleRuleDelete(String key) {
        clearRuleKeys(key);
        log.info("Redis单条规则删除完成: key={}", key);
    }

    /**
     * 清理指定规则的所有Redis key
     * 
     * @param key 资源标识
     */
    private void clearRuleKeys(String key) {
        // 滑动窗口算法使用的key
        String slidingWindowKey = "rate_limit:" + key;
        
        // 令牌桶算法使用的key
        String tokenBucketTokensKey = "rate_limit:" + key + "_tokens";
        String tokenBucketTimestampKey = "rate_limit:" + key + "_timestamp";
        
        // 删除所有相关key
        redisTemplate.delete(slidingWindowKey);
        redisTemplate.delete(tokenBucketTokensKey);
        redisTemplate.delete(tokenBucketTimestampKey);
    }

    /**
     * 清理所有限流相关的Redis key
     */
    private void clearAllRateLimitKeys() {
        // 使用模式匹配删除所有以"rate_limit:"开头的key
        redisTemplate.delete(redisTemplate.keys("rate_limit:*"));
        log.info("已清理所有限流相关的Redis key");
    }

    /**
     * 初始化规则到Redis
     * 
     * @param key 资源标识
     * @param rule 限流规则
     */
    private void initializeRule(String key, RateLimitRule rule) {
        // 根据算法类型初始化不同的数据结构
        switch (rule.getAlgorithmType()) {
            case SLIDING_WINDOW:
                // 滑动窗口算法：不需要特殊初始化，Lua脚本会处理
                log.debug("滑动窗口算法规则初始化: key={}", key);
                break;
            case TOKEN_BUCKET:
                // 令牌桶算法：初始化令牌数量和最后更新时间
                initializeTokenBucket(key, rule);
                break;
            default:
                log.warn("不支持的算法类型: {}, key={}", rule.getAlgorithmType(), key);
                break;
        }
    }

    /**
     * 初始化令牌桶算法
     * 
     * @param key 资源标识
     * @param rule 限流规则
     */
    private void initializeTokenBucket(String key, RateLimitRule rule) {
        String tokensKey = "rate_limit:" + key + "_tokens";
        String timestampKey = "rate_limit:" + key + "_timestamp";
        
        // 初始化令牌数量为最大容量
        redisTemplate.opsForValue().set(tokensKey, rule.getLimit());
        
        // 初始化最后更新时间戳为当前时间
        long currentTime = System.currentTimeMillis();
        redisTemplate.opsForValue().set(timestampKey, currentTime);
        
        log.debug("令牌桶算法规则初始化完成: key={}, limit={}", key, rule.getLimit());
    }
}