package com.xiao.ratelimiterconfig.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xiao.ratelimitercore.algorithm.AlgorithmType;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RateLimitConfig {
    
    @JsonProperty("rate-limit")
    private RateLimit rateLimit;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RateLimit {
        private boolean enabled = true;
        
        private FallbackConfig fallback;
        
        private GlobalConfig global;
        
        private List<Rule> rules;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FallbackConfig {
        private boolean enabled = true;
        private int limit = 200;
        private int window = 1;
        private AlgorithmType algorithm = AlgorithmType.LOCAL;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GlobalConfig {
        private boolean enabled=true;
        private int limit = 50;
        private int window = 1;
        private AlgorithmType algorithm = AlgorithmType.SLIDING_WINDOW;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rule {
        private String key;
        private int limit;
        private int window;
        private AlgorithmType algorithm;
    }
}