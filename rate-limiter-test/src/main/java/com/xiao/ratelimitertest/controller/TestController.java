package com.xiao.ratelimitertest.controller;

import com.xiao.ratelimiterspringbootstarter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@Slf4j
public class TestController {

    /**
     * 基础限流测试 - 每秒最多5次请求
     */
    @GetMapping("/basic")
    @RateLimiter(key = "api:test:basic", limit = 5, window = 1)
    public String basicRateLimit() {
        log.info("Basic rate limit test - Request allowed");
        return "Basic rate limit test - Request allowed at " + System.currentTimeMillis();
    }


    /**
     * 用户级限流测试 - 每个用户每秒最多3次请求
     */
    @GetMapping("/user/{userId}")
    @RateLimiter(key = "'api:test:user:' + #userId", limit = 3, window = 1)
    public String userRateLimit(@PathVariable String userId) {
        log.info("User rate limit test - User: {}, Request allowed", userId);
        return String.format("User %s - Request allowed at %d", userId, System.currentTimeMillis());
    }

    /**
     * 高频率限流测试 - 每秒最多10次请求
     */
    @GetMapping("/high-frequency")
    @RateLimiter(key = "test:high-frequency", limit = 10, window = 1)
    public String highFrequencyRateLimit() {
        log.info("High frequency rate limit test - Request allowed");
        return "High frequency rate limit test - Request allowed at " + System.currentTimeMillis();
    }

    /**
     * 无限制测试 - 用于对比
     */
    @GetMapping("/unlimited")
    public String unlimited() {
        log.info("Unlimited test - Request allowed");
        return "Unlimited test - Request allowed at " + System.currentTimeMillis();
    }
}