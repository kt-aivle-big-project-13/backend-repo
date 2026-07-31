package com.aivle13.fin_audit_ai.domain.audit.dto.response.explainability;

import com.aivle13.fin_audit_ai.domain.audit.entity.ShapFeatureImportanceEntity;

import java.math.BigDecimal;

public record FeatureImportanceResponse(
        Integer rank,
        String feature,
        BigDecimal value,
        boolean isSensitive,
        String sensitiveGroup
) {

    public static FeatureImportanceResponse from(ShapFeatureImportanceEntity entity) {
        return new FeatureImportanceResponse(
                entity.getRank(),
                entity.getFeature(),
                entity.getContributionRatio(),
                entity.isSensitive(),
                entity.getSensitiveGroup()
        );
    }
}
