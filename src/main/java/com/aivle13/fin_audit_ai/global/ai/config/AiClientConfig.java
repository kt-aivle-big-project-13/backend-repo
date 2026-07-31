package com.aivle13.fin_audit_ai.global.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AiServerProperties.class)
public class AiClientConfig {

    @Bean
    public RestClient shapRestClient(
            AiServerProperties properties
    ) {
        return createRestClient(
                properties,
                properties.readTimeout()
        );
    }

    // 리포트 생성(설명가능성·편향)은 분석에 더해 LLM 서술·figure 생성·HTML→PDF 렌더까지
    // 한 요청에서 처리해 일반 분석 호출보다 오래 걸리므로 읽기 타임아웃을 따로 둔다.
    @Bean
    public RestClient reportRestClient(
            AiServerProperties properties
    ) {
        return createRestClient(
                properties,
                properties.reportReadTimeout()
        );
    }

    private RestClient createRestClient(
            AiServerProperties properties,
            Duration readTimeout
    ) {
        // JDK HttpClient 기본값은 HTTP/2라 평문 연결에서 h2c 업그레이드를 시도한다.
        // Uvicorn(FastAPI)은 h2c를 지원하지 않아 요청이 깨지므로 HTTP/1.1로 고정한다.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }
}