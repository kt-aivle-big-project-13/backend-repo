package com.aivle13.fin_audit_ai.domain.audit.entity;

import com.aivle13.fin_audit_ai.domain.audit.type.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 감사의 공정성(편향) 지표별 측정 결과.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fairness_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fairness_results_audit_metric",
                columnNames = {"audit_id", "metric_code"}))
public class FairnessResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id")
    private AuditEntity audit;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_code", nullable = false, length = 30)
    private FairnessMetricCode metricCode;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal value;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal threshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FairnessStatus status;
}
