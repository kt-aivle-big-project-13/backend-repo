package com.aivle13.fin_audit_ai.health;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthCheckRepository healthCheckRepository;

    @GetMapping
    public HealthResponse check() {
        HealthCheck saved = healthCheckRepository.save(HealthCheck.create());
        return new HealthResponse("OK", saved.getId(), saved.getCreatedAt());
    }

    public record HealthResponse(String status, Long id, LocalDateTime checkedAt) {
    }
}