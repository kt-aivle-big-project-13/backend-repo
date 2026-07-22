package com.aivle13.fin_audit_ai.global.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai-server")
public record AiServerProperties(
        boolean enabled,
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}