package com.platform.orderservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.inventory")
public record InventoryClientProperties(
    String url, Duration connectTimeout, Duration readTimeout) {}
