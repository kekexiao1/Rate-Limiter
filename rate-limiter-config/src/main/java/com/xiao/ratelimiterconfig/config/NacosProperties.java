package com.xiao.ratelimiterconfig.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limiter")
@Data
public class NacosProperties {

	private boolean enabled;

	private String dataId="rate-limiter-rules.yaml";

	private String group="DEFAULT_GROUP";
}
