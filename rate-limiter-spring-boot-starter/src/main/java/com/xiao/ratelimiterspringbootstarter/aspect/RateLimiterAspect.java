package com.xiao.ratelimiterspringbootstarter.aspect;

import com.xiao.ratelimiterfallback.algorithm.LocalRateLimitAlgorithm;
import com.xiao.ratelimiterredis.algorithm.RedisAlgorithmFactory;
import com.xiao.ratelimitercore.exception.RedisUnavailableException;
import com.xiao.ratelimiterredis.monitor.RedisHealthMonitor;
import com.xiao.ratelimiterspringbootstarter.annotation.RateLimiter;
import com.xiao.ratelimitercore.algorithm.AlgorithmType;
import com.xiao.ratelimitercore.algorithm.RateLimitAlgorithm;
import com.xiao.ratelimitercore.exception.RateLimitException;
import com.xiao.ratelimitercore.model.RateLimitResult;
import com.xiao.ratelimitercore.model.RateLimitRule;
import com.xiao.ratelimiterconfig.manager.RateLimitRuleManager;
import com.xiao.ratelimiterfallback.fallback.RateLimiterFallbackHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * 限流切面
 * 拦截@RateLimiter注解的方法，实现限流逻辑
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class RateLimiterAspect {

    private final RateLimitRuleManager ruleManager;
    private final RedisAlgorithmFactory algorithmFactory;
    private final RateLimitAlgorithm localRateLimitAlgorithm;
    private final RedisHealthMonitor redisHealthMonitor; // 注入我们之前设计的状态位
    
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    /**
     * 环绕通知，拦截@RateLimiter注解的方法
     */
    @Around("@annotation(com.xiao.ratelimiterspringbootstarter.annotation.RateLimiter)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimiter rateLimiter = AnnotationUtils.findAnnotation(method, RateLimiter.class);
        
        if (rateLimiter == null) {
            return joinPoint.proceed();
        }

        // 验证注解参数
        validateRateLimiterAnnotation(rateLimiter);

        // 获取限流参数，支持SpEL表达式
        String rawKey = rateLimiter.key();

        String key=evaluateSpelExpression(rawKey, joinPoint, method);;

        RateLimitRule rule=null;
        if(redisHealthMonitor.isRedisAvailable()){
            // 1. 尝试从Nacos获取规则（本地缓存）
            if(ruleManager.containsRule(rawKey)){
                rule = ruleManager.getRule(rawKey);
                log.debug("使用Nacos配置的限流规则: key={}, limit={}, window={}, algorithm={}",
                        key, rule.getLimit(), rule.getWindow(), rule.getAlgorithmType());
            }else{
                rule = new RateLimitRule(key, (int) rateLimiter.limit(), rateLimiter.window(), rateLimiter.type());
                log.debug("使用注解配置的限流规则: key={}, limit={}, window={}",
                        key, rateLimiter.limit(), rateLimiter.window());
            }
        }else{
            // 使用本地限流算法
            rule = ruleManager.getRule("fallback");
            log.debug("使用本地配置的限流规则: key={}, limit={}, window={}, algorithm={}",
                    rule.getKey(), rule.getLimit(), rule.getWindow(), rule.getAlgorithmType());
        }

        RateLimitResult result = limitChecked(key, rule);

        if (result.isAllowed()) {
            log.debug("限流检查通过: key={}, remaining={}", key, result.getRemaining());
            return joinPoint.proceed();
        } else {
            log.warn("限流检查拒绝: key={}, waitMs={}", key, result.getWaitMs());
            throw new RateLimitException(
                    key,
                    rule.getLimit(),
                    rule.getWindow(),
                    result.getWaitMs(),
                    "请求过于频繁，请稍后重试"
            );
        }
    }

    /**
     * 限流检查
     */
    private RateLimitResult limitChecked(String key, RateLimitRule rule){
        // 根据算法类型选择限流算法
        RateLimitAlgorithm algorithm = selectAlgorithm(rule.getAlgorithmType());
        // 执行限流检查
        RateLimitResult result = null;
        try {
            result = algorithm.tryAcquire(key, rule);
            return result;
        } catch (RedisUnavailableException e) {
            log.warn("Redis不可用，自动降级到本地限流算法: key={}, error={}", key, e.getMessage());
            if(localRateLimitAlgorithm != null) {
                result = localRateLimitAlgorithm.tryAcquire(key, rule);
                log.info("已降级到本地限流算法，限流检查完成: key={}", key);
                return result;
            } else {
                log.error("本地限流算法不可用，无法降级，直接拒绝请求: key={}", key);
                throw new RateLimitException(
                        key,
                        rule.getLimit(),
                        rule.getWindow(),
                        0,
                        "Redis不可用且本地降级算法未配置，请求被拒绝"
                );
            }
        }
    }

    /**
     * 根据算法类型选择限流算法
     */
    private RateLimitAlgorithm selectAlgorithm(AlgorithmType algorithmType) {
        switch (algorithmType) {
            case SLIDING_WINDOW:
                return algorithmFactory.getAlgorithmByType(AlgorithmType.SLIDING_WINDOW);
            case TOKEN_BUCKET:
                return algorithmFactory.getAlgorithmByType(AlgorithmType.TOKEN_BUCKET);
            case LOCAL:
                return algorithmFactory.getAlgorithmByType(AlgorithmType.LOCAL);
            default:
                log.warn("不支持的算法类型: {}, 使用默认的滑动窗口算法", algorithmType);
                return algorithmFactory.getAlgorithmByType(AlgorithmType.SLIDING_WINDOW);
        }
    }

    /**
     * 解析并评估SpEL表达式
     */
    private String evaluateSpelExpression(String expression, ProceedingJoinPoint joinPoint, Method method) {
        // 如果表达式不包含SpEL语法，直接返回原值
        if (!containsSpelExpression(expression)) {
            return expression;
        }

        try {
            // 创建评估上下文
            StandardEvaluationContext context = new StandardEvaluationContext();

            // 设置方法参数
            Object[] args = joinPoint.getArgs();
            String[] parameterNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();

            if (parameterNames != null && args != null) {
                for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }

            // 设置方法本身
            context.setVariable("method", method);
            context.setVariable("target", joinPoint.getTarget());

            // 解析并替换字符串中的SpEL表达式片段
            String result = parseAndReplaceSpelFragments(expression, context);

            if (result == null) {
                log.warn("SpEL表达式评估结果为null，使用原始表达式: {}", expression);
                return expression;
            }

            return result;

        } catch (Exception e) {
            log.warn("SpEL表达式评估失败，使用原始表达式: {}, 错误: {}", expression, e.getMessage());
            return expression;
        }
    }

    /**
     * 判断是否包含SpEL表达式
     */
    private boolean containsSpelExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        // 检测是否包含SpEL表达式模式
        return expression.contains("#{") ||
               expression.contains("$") ||
               expression.contains("T(") ||
               expression.contains("@") ||
               expression.contains("{#") ||
               expression.contains("${#");
    }

    /**
     * 解析并替换字符串中的SpEL表达式片段
     */
    private String parseAndReplaceSpelFragments(String input, StandardEvaluationContext context) {
        StringBuilder result = new StringBuilder();
        int startIndex = 0;
        int currentIndex = 0;

        while (currentIndex < input.length()) {
            // 查找SpEL表达式开始标记
            int spelStart = input.indexOf("#{", currentIndex);
            if (spelStart == -1) {
                // 没有更多SpEL表达式，添加剩余部分
                result.append(input.substring(currentIndex));
                break;
            }

            // 添加SpEL表达式之前的部分
            result.append(input.substring(currentIndex, spelStart));

            // 查找SpEL表达式结束标记
            int spelEnd = findSpelEnd(input, spelStart + 2);
            if (spelEnd == -1) {
                // 没有找到结束标记，添加剩余部分并退出
                result.append(input.substring(spelStart));
                break;
            }

            // 提取SpEL表达式内容
            String spelExpression = input.substring(spelStart + 2, spelEnd);

            try {
                // 评估SpEL表达式
                Expression expression = SPEL_PARSER.parseExpression(spelExpression);
                Object evaluated = expression.getValue(context);
                if (evaluated != null) {
                    result.append(evaluated.toString());
                } else {
                    // 如果评估结果为null，保留原始表达式
                    result.append("#{").append(spelExpression).append("}");
                }
            } catch (Exception e) {
                // 评估失败，保留原始表达式
                log.warn("SpEL表达式片段评估失败: #{}, 错误: {}", spelExpression, e.getMessage());
                result.append("#{").append(spelExpression).append("}");
            }

            currentIndex = spelEnd + 1;
        }

        return result.toString();
    }

    /**
     * 查找SpEL表达式的结束位置
     */
    private int findSpelEnd(String input, int startIndex) {
        int braceCount = 1; // 已经有一个开括号

        for (int i = startIndex; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    return i;
                }
            }
        }

        return -1; // 没有找到匹配的结束括号
    }

    /**
     * 验证@RateLimiter注解参数
     */
    private void validateRateLimiterAnnotation(RateLimiter rateLimiter) {
        // 验证key不能为空
        if (rateLimiter.key() == null || rateLimiter.key().trim().isEmpty()) {
            throw new IllegalArgumentException("@RateLimiter注解的key参数不能为空");
        }

        // 验证key长度限制（对于SpEL表达式，长度限制可以放宽）
        if (!containsSpelExpression(rateLimiter.key()) && rateLimiter.key().trim().length() > 100) {
            throw new IllegalArgumentException("@RateLimiter注解的key长度不能超过100字符");
        }

        // 验证限流阈值必须大于0
        if (rateLimiter.limit() <= 0) {
            throw new IllegalArgumentException("@RateLimiter注解的limit参数必须大于0，当前值: " + rateLimiter.limit());
        }

        // 验证时间窗口必须大于0
        if (rateLimiter.window() <= 0) {
            throw new IllegalArgumentException("@RateLimiter注解的window参数必须大于0，当前值: " + rateLimiter.window());
        }

        // 验证算法类型不能为空
        if (rateLimiter.type() == null) {
            throw new IllegalArgumentException("@RateLimiter注解的type参数不能为空");
        }

        // 验证降级处理器类必须实现RateLimiterFallbackHandler接口
        if (rateLimiter.fallbackHandler() != null && !RateLimiterFallbackHandler.class.isAssignableFrom(rateLimiter.fallbackHandler())) {
            throw new IllegalArgumentException("@RateLimiter注解的fallbackHandler参数必须实现RateLimiterFallbackHandler接口");
        }

        log.debug("@RateLimiter注解参数验证通过: key={}, limit={}, window={}, type={}", 
                rateLimiter.key(), rateLimiter.limit(), rateLimiter.window(), rateLimiter.type());
    }
}