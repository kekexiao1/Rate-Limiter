package com.xiao.ratelimiterredis;

import com.xiao.ratelimitercore.algorithm.RateLimitAlgorithm;
import com.xiao.ratelimiterredis.algorithm.RedisAlgorithmFactory;
import com.xiao.ratelimiterredis.executor.RedisScriptExecutor;
import com.xiao.ratelimiterredis.monitor.RedisHealthMonitor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Redis自动配置类
 * 提供Redis相关的Bean配置
 */
@AutoConfiguration
@ConditionalOnClass(RedisTemplate.class)
@EnableScheduling
public class RedisAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory);
		
		// 设置key的序列化器
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		
		// 设置value的序列化器
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
		
		template.afterPropertiesSet();
		return template;
	}

	@Bean
	public RedisHealthMonitor redisHealthMonitor(RedisConnectionFactory connectionFactory) {
		return new RedisHealthMonitor(connectionFactory);
	}

	@Bean
	@ConditionalOnBean(RedisTemplate.class)
	public RedisScriptExecutor redisScriptExecutor(RedisTemplate<String, Object> redisTemplate) {
		return new RedisScriptExecutor(redisTemplate);
	}

	@Bean
	@ConditionalOnBean(RedisScriptExecutor.class)
	public RedisAlgorithmFactory redisAlgorithmFactory(RedisScriptExecutor scriptExecutor) {
		return new RedisAlgorithmFactory(scriptExecutor);
	}

	@Bean
	@ConditionalOnBean(RedisAlgorithmFactory.class)
	public RateLimitAlgorithm redisSlidingWindowAlgorithm(RedisAlgorithmFactory factory) {
		return factory.getAlgorithmByType(com.xiao.ratelimitercore.algorithm.AlgorithmType.SLIDING_WINDOW);
	}

	@Bean
	@ConditionalOnBean(RedisAlgorithmFactory.class)
	public RateLimitAlgorithm redisTokenBucketAlgorithm(RedisAlgorithmFactory factory) {
		return factory.getAlgorithmByType(com.xiao.ratelimitercore.algorithm.AlgorithmType.TOKEN_BUCKET);
	}

}
