package com.aivle13.fin_audit_ai;

import com.aivle13.fin_audit_ai.health.HealthCheck;
import com.aivle13.fin_audit_ai.health.HealthCheckRepository;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class InfraIntegrationTest extends IntegrationTestSupport {

    @Autowired HealthCheckRepository healthCheckRepository;
    @Autowired StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("컨텍스트가 로딩되고 PostgreSQL에 읽고 쓸 수 있다")
    void postgres() {
        HealthCheck saved = healthCheckRepository.save(HealthCheck.create());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();   // JPA Auditing 동작 검증
        assertThat(healthCheckRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("Redis에 읽고 쓸 수 있다")
    void redis() {
        redisTemplate.opsForValue().set("test:key", "hello");

        assertThat(redisTemplate.opsForValue().get("test:key")).isEqualTo("hello");
    }
}