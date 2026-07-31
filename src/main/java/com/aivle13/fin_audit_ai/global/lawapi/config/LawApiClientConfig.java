package com.aivle13.fin_audit_ai.global.lawapi.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * law.go.kr은 평문 HTTP 정부 API라 SHAP/AI 서버(AiClientConfig)처럼 h2c 업그레이드를
 * 신경 쓸 필요가 없어, JDK HttpClient 대신 더 단순한 SimpleClientHttpRequestFactory를 쓴다.
 */
@Configuration
@EnableConfigurationProperties(LawApiProperties.class)
public class LawApiClientConfig {

    @Bean
    public RestClient lawApiRestClient(LawApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }
}
