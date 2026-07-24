package com.aivle13.fin_audit_ai.global.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(AiServerProperties.class)
public class AiClientConfig {

    @Bean
    public RestClient shapRestClient(
            AiServerProperties properties
    ) {
        // JDK HttpClient 기본값은 HTTP/2라 평문 연결에서 h2c 업그레이드를 시도한다.
        // Uvicorn(FastAPI)은 h2c를 지원하지 않아 요청이 깨지므로 HTTP/1.1로 고정한다.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }
}