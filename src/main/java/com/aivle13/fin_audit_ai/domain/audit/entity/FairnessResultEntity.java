package com.aivle13.fin_audit_ai.domain.audit.entity;

import com.aivle13.fin_audit_ai.domain.audit.type.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 감사의 공정성(편향) 지표 측정 결과.
 * 보호속성(성별·연령대 등) × 지표(DP·EO·EOdd) 조합마다 한 행씩 저장한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fairness_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fairness_results_audit_metric_attribute",
                columnNames = {"audit_id", "metric_code", "attribute"}))
public class FairnessResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id")
    private AuditEntity audit;

    // 보호속성 컬럼명. AI 응답 fairness_by_attribute 의 키와 동일 (예: CODE_GENDER, AGE_GROUP)
    @Column(name = "attribute", nullable = false, length = 50)
    private String attribute;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_code", nullable = false, length = 30)
    private FairnessMetricCode metricCode;

    // AI가 준 지표값 (해당 보호속성 기준 집단 간 최대 격차)
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal value;

    // 판정 기준값 (정책값, 백엔드가 채움)
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal threshold;

    // 이 (보호속성, 지표) 행의 판정 — PASS / REVIEW / FAIL
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FairnessStatus status;

    public static FairnessResultEntity of(AuditEntity audit, String attribute,
                                           FairnessMetricCode metricCode, BigDecimal value,
                                           BigDecimal threshold, FairnessStatus status) {
        FairnessResultEntity result = new FairnessResultEntity();
        result.audit = audit;
        result.attribute = attribute;
        result.metricCode = metricCode;
        result.value = value;
        result.threshold = threshold;
        result.status = status;
        return result;
    }
}
