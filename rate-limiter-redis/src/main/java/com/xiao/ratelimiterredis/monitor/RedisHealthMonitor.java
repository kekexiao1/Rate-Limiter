package com.xiao.ratelimiterredis.monitor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis健康检查监控器
 * 定期检查Redis连接状态，用于提前发现Redis不可用的情况
 */
@Slf4j
public class RedisHealthMonitor implements DisposableBean {
    
    private final RedisConnectionFactory connectionFactory;
    private final AtomicBoolean redisAvailable = new AtomicBoolean(true);
    private final AtomicBoolean lastCheckResult = new AtomicBoolean(true);

    private final ScheduledExecutorService executor= Executors.newScheduledThreadPool(1);
    private final AtomicBoolean started = new AtomicBoolean(false);

    public RedisHealthMonitor(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
    
    /**
     * 定期检查Redis连接状态（每5秒检查一次）
     */
    @PostConstruct
    public void checkRedisHealth() {
        if(started.compareAndSet(false, true)){
            executor.scheduleAtFixedRate(() -> {
                boolean isHealthy = pingRedis();
                boolean wasAvailable = redisAvailable.getAndSet(isHealthy);

                if (wasAvailable && !isHealthy) {
                    log.error("Redis连接检查失败，Redis不可用，将自动降级到本地限流算法");
                } else if (!wasAvailable && isHealthy) {
                    log.info("Redis连接已恢复，将恢复使用Redis限流算法");
                }
                lastCheckResult.set(isHealthy);
            }, 1, 5, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 检查Redis是否可用
     * 
     * @return true表示Redis可用，false表示不可用
     */
    public boolean isRedisAvailable() {
        return redisAvailable.get();
    }
    
    /**
     * 获取最后一次检查的结果
     * 
     * @return 最后一次检查结果
     */
    public boolean getLastCheckResult() {
        return lastCheckResult.get();
    }
    
    /**
     * 主动ping Redis检查连接
     * 
     * @return true表示连接正常，false表示连接失败
     */
    private boolean pingRedis() {
        try (RedisConnection conn = connectionFactory.getConnection()) {
            conn.ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }
}
