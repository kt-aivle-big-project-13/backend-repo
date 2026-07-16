package com.aivle13.fin_audit_ai.health;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime checkedAt;

    public static HealthCheck now() {
        HealthCheck h = new HealthCheck();
        h.checkedAt = LocalDateTime.now();
        return h;
    }
}