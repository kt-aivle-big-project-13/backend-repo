package com.aivle13.fin_audit_ai.domain.audit.entity;

import com.aivle13.fin_audit_ai.domain.audit.type.XaiMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 감사의 설명가능성(XAI) 지표별 측정 결과.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "xai_results")
public class XaiResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id")
    private AuditEntity audit;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_code", nullable = false, length = 30)
    private XaiMetricCode metricCode;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal value;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal threshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private XaiStatus status;
}
