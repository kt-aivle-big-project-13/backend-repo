package com.aivle13.fin_audit_ai.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 접근: http://localhost:8080/swagger-ui.html
 * Security 설정에서 Swagger 관련 경로(/swagger-ui/**, /v3/api-docs/**)를 인증 없이 접근 허용(permitAll)해줘야 합니다.
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI passPointOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("신용평가 AI 규제 준수 감사 플랫폼 API")
                        .description("AI 기본법 / 금융분야 AI 가이드라인 준수 여부 자동 감사 API")
                        .version("0.0.1"))
                // JWT 인증 스킴 등록
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
