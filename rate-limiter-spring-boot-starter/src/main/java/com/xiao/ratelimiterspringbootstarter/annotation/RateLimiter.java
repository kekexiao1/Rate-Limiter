package com.xiao.ratelimiterspringbootstarter.annotation;

import com.xiao.ratelimitercore.algorithm.AlgorithmType;
import com.xiao.ratelimiterfallback.fallback.DefaultFallbackHandler;
import com.xiao.ratelimiterfallback.fallback.RateLimiterFallbackHandler;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {

	/**
	 * 限流的接口key，支持SpEL表达式
	 * 例如：
	 * - 静态值: "api:user:login"
	 * - 方法参数: "#userId"
	 * - 组合参数: "api:user:#userId:operation"
	 * - 方法名: "#method.name"
	 */
	String key() default "";

	/**
	 * 限流阈值,limit: 限制数量,window:时间窗口,单位秒,type: 算法类型
	 * @return
	 */
	double limit() default 50;
	int window() default 1;
	AlgorithmType type() default AlgorithmType.SLIDING_WINDOW;

	/**
	 * 被限流时的抛出异常 or 执行降级方法
	 */
	Class<? extends RateLimiterFallbackHandler> fallbackHandler() default DefaultFallbackHandler.class;
}
