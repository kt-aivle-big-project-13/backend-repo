package com.aivle13.fin_audit_ai.global.config;

import com.aivle13.fin_audit_ai.global.jwt.JwtAccessDeniedHandler;
import com.aivle13.fin_audit_ai.global.jwt.JwtAuthenticationEntryPoint;
import com.aivle13.fin_audit_ai.global.jwt.JwtAuthenticationFilter;
import com.aivle13.fin_audit_ai.global.jwt.JwtProperties;
import com.aivle13.fin_audit_ai.global.jwt.JwtProvider;
import tools.jackson.databind.ObjectMapper;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",

            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            // 시연용 게스트 발급. app.demo.enabled 가 꺼져 있으면 404 로 응답한다.
            "/api/v1/auth/demo",
            "/api/v1/auth/reissue",
            "/api/v1/auth/password/find",
            "/api/v1/auth/password/reset",
            "/api/v1/auth/email/verification-code",
            "/api/v1/auth/email/verification-code/confirm"
    };

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtProvider jwtProvider,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.jwtProvider = jwtProvider;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // 공지사항 조회는 로그인 사용자 전체, 작성·수정·삭제는 관리자만 가능
                        .requestMatchers(HttpMethod.POST, "/api/v1/posts").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/posts/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/posts/*").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(new JwtAccessDeniedHandler(objectMapper))
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, redisTemplate),
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
