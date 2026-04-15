package com.xiao.ratelimiterredis.algorithm;

import com.xiao.ratelimitercore.algorithm.AlgorithmType;
import com.xiao.ratelimitercore.algorithm.RateLimitAlgorithm;
import com.xiao.ratelimiterfallback.algorithm.LocalRateLimitAlgorithm;
import com.xiao.ratelimiterredis.executor.RedisScriptExecutor;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis算法工厂类
 * 负责创建和管理基于Redis的限流算法实例
 */
@Slf4j
public class RedisAlgorithmFactory {

	private final ConcurrentHashMap<AlgorithmType, RateLimitAlgorithm> algorithmMap = new ConcurrentHashMap<>();

	public RedisAlgorithmFactory(RedisScriptExecutor scriptExecutor) {
		// 初始化所有支持的算法
		algorithmMap.put(AlgorithmType.SLIDING_WINDOW, new RedisSlidingWindowAlgorithm(scriptExecutor));
		algorithmMap.put(AlgorithmType.TOKEN_BUCKET, new RedisTokenBucketAlgorithm(scriptExecutor));
		algorithmMap.put(AlgorithmType.LOCAL, new LocalRateLimitAlgorithm());
	}

	/**
	 * 根据算法类型获取限流算法实例
	 * 
	 * @param type 算法类型
	 * @return 限流算法实例，如果类型不支持则返回null
	 */
	public RateLimitAlgorithm getAlgorithmByType(AlgorithmType type) {
		RateLimitAlgorithm algorithm = algorithmMap.get(type);
		if (algorithm == null) {
			log.error("获取的限流算法为空，算法类型: {}", type);
		}
		return algorithm;
	}

	/**
	 * 检查是否支持指定的算法类型
	 * 
	 * @param type 算法类型
	 * @return true表示支持，false表示不支持
	 */
	public boolean supportsAlgorithm(AlgorithmType type) {
		return algorithmMap.containsKey(type);
	}

	/**
	 * 获取所有支持的算法类型
	 * 
	 * @return 支持的算法类型集合
	 */
	public Set<AlgorithmType> getSupportedAlgorithms() {
		return algorithmMap.keySet();
	}
}
