package com.aivle13.fin_audit_ai.global.lawapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.law-api")
public record LawApiProperties(
        boolean enabled,
        String ocKey,
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
