package com.aivle13.fin_audit_ai.global.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenValidity,
        long refreshTokenValidity,
        long rememberMeRefreshTokenValidity
) {
}