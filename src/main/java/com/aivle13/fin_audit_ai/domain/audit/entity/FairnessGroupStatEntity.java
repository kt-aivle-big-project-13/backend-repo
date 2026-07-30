package com.aivle13.fin_audit_ai.domain.audit.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 감사의 보호속성별 집단(예: 성별 M/F) 하나의 기초 통계·혼동행렬.
 * 지표 단위인 {@link FairnessResultEntity} 와 달리 집단(group) 단위로 저장한다.
 * 혼동행렬은 favorable(승인=유리) 관점 — 정상 승인=TP, 연체 승인=FP(오승인),
 * 연체 거절=TN, 정상 거절=FN(오거절). FPR/FDR/FOR Parity 의 원자료다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fairness_group_stats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fairness_group_stats_audit_attribute_group",
                columnNames = {"audit_id", "attribute", "group_name"}))
public class FairnessGroupStatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_stat_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id")
    private AuditEntity audit;

    // 보호속성 컬럼명 (예: CODE_GENDER, AGE_GROUP)
    @Column(name = "attribute", nullable = false, length = 50)
    private String attribute;

    // 집단 값 (예: M, F, 30대). group 은 SQL 예약어라 group_name 으로 매핑한다.
    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "n", nullable = false)
    private int n;

    @Column(name = "approval_rate", precision = 10, scale = 4)
    private BigDecimal approvalRate;

    @Column(name = "actual_default_rate", precision = 10, scale = 4)
    private BigDecimal actualDefaultRate;

    @Column(name = "tp", nullable = false)
    private int tp;

    @Column(name = "fp", nullable = false)
    private int fp;

    @Column(name = "tn", nullable = false)
    private int tn;

    @Column(name = "fn", nullable = false)
    private int fn;

    // 집단 내 위험점수 AUC. 한 클래스만 있거나 점수가 없으면 null.
    @Column(name = "auc", precision = 6, scale = 4)
    private BigDecimal auc;

    public static FairnessGroupStatEntity of(
            AuditEntity audit, String attribute, String groupName, int n,
            BigDecimal approvalRate, BigDecimal actualDefaultRate,
            int tp, int fp, int tn, int fn, BigDecimal auc) {
        FairnessGroupStatEntity stat = new FairnessGroupStatEntity();
        stat.audit = audit;
        stat.attribute = attribute;
        stat.groupName = groupName;
        stat.n = n;
        stat.approvalRate = approvalRate;
        stat.actualDefaultRate = actualDefaultRate;
        stat.tp = tp;
        stat.fp = fp;
        stat.tn = tn;
        stat.fn = fn;
        stat.auc = auc;
        return stat;
    }
}
