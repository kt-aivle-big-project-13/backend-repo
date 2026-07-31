package com.aivle13.fin_audit_ai.domain.audit.dto.response.explainability;

import com.aivle13.fin_audit_ai.domain.audit.entity.ShapFeatureImportanceEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiMetricCode;

import java.util.Comparator;
import java.util.List;

public record ExplainabilityResponse(
        Long auditId,
        String method,
        List<XaiMetricResponse> metrics,
        List<FeatureImportanceResponse> topFeatures
) {

    public static ExplainabilityResponse of(
            Long auditId,
            List<XaiResultEntity> results,
            List<ShapFeatureImportanceEntity> topFeatures
    ) {
        List<XaiMetricResponse> metrics = results.stream()
                .sorted(Comparator.comparingInt(
                        result -> metricOrder(result.getMetricCode())
                ))
                .map(XaiMetricResponse::from)
                .toList();

        return new ExplainabilityResponse(
                auditId,
                "SHAP",
                metrics,
                topFeatures.stream()
                        .map(FeatureImportanceResponse::from)
                        .toList()
        );
    }

    private static int metricOrder(XaiMetricCode metricCode) {
        return switch (metricCode) {
            case SENSITIVE_CONTRIB -> 0;
            case GLOBAL_STABILITY -> 1;
            case FIDELITY -> 2;
            default -> Integer.MAX_VALUE;
        };
    }
}