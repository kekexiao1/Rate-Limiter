package com.xiao.ratelimiterconfig.event;

import com.xiao.ratelimitercore.model.RateLimitRule;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 限流规则变更事件
 * 当Nacos配置发生变更时发布此事件
 */
@Getter
public class RateLimitRuleChangeEvent extends ApplicationEvent {
    
    /**
     * 变更后的规则集合
     */
    private final Map<String, RateLimitRule> rules;
    
    /**
     * 变更类型
     */
    private final ChangeType changeType;
    
    /**
     * 变更的资源标识（仅对单条规则变更有效）
     */
    private final String changedKey;
    
    public RateLimitRuleChangeEvent(Object source, Map<String, RateLimitRule> rules, ChangeType changeType) {
        this(source, rules, changeType, null);
    }
    
    public RateLimitRuleChangeEvent(Object source, Map<String, RateLimitRule> rules, ChangeType changeType, String changedKey) {
        super(source);
        this.rules = rules;
        this.changeType = changeType;
        this.changedKey = changedKey;
    }
    
    /**
     * 变更类型枚举
     */
    public enum ChangeType {
        /**
         * 全量更新（首次加载或配置完全变更）
         */
        FULL_UPDATE,
        
        /**
         * 单条规则添加
         */
        ADD,
        
        /**
         * 单条规则更新
         */
        UPDATE,
        
        /**
         * 单条规则删除
         */
        DELETE
    }
}