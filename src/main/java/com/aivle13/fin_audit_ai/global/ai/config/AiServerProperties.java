package com.aivle13.fin_audit_ai.global.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai-server")
public record AiServerProperties(
        boolean enabled,
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout,

        // 리포트 생성은 분석·LLM 서술·figure 생성·PDF 렌더를 한 요청에서 처리해
        // 다른 AI 호출보다 훨씬 오래 걸리므로 별도 읽기 타임아웃을 둔다.
        Duration reportReadTimeout
) {
}