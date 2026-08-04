package com.platform.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limiter")
public record RateLimiterProperties(boolean enabled, int capacity, double refillRatePerSecond) {}
