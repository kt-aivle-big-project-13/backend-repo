package com.aivle13.fin_audit_ai.domain.audit.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 감사의 SHAP 전역 피처 중요도 랭킹 한 줄 (대시보드 "예측 영향 변수 TOP N" 카드용).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "shap_feature_importances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_shap_feature_importances_audit_rank",
                columnNames = {"audit_id", "rank"}
        )
)
public class ShapFeatureImportanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "importance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id")
    private AuditEntity audit;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @Column(nullable = false, length = 100)
    private String feature;

    @Column(name = "mean_abs_shap", nullable = false, precision = 14, scale = 6)
    private BigDecimal meanAbsShap;

    @Column(name = "mean_signed_shap", nullable = false, precision = 14, scale = 6)
    private BigDecimal meanSignedShap;

    @Column(name = "contribution_ratio", precision = 10, scale = 6)
    private BigDecimal contributionRatio;

    @Column(length = 20)
    private String direction;

    @Column(name = "is_sensitive", nullable = false)
    private boolean sensitive;

    @Column(name = "sensitive_group", length = 50)
    private String sensitiveGroup;

    public static ShapFeatureImportanceEntity of(
            AuditEntity audit,
            Integer rank,
            String feature,
            BigDecimal meanAbsShap,
            BigDecimal meanSignedShap,
            BigDecimal contributionRatio,
            String direction,
            boolean sensitive,
            String sensitiveGroup
    ) {
        ShapFeatureImportanceEntity entity = new ShapFeatureImportanceEntity();
        entity.audit = audit;
        entity.rank = rank;
        entity.feature = feature;
        entity.meanAbsShap = meanAbsShap;
        entity.meanSignedShap = meanSignedShap;
        entity.contributionRatio = contributionRatio;
        entity.direction = direction;
        entity.sensitive = sensitive;
        entity.sensitiveGroup = sensitiveGroup;
        return entity;
    }
}
