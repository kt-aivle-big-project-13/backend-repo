package com.aivle13.fin_audit_ai.domain.audit.dto.response.explainability;

import com.aivle13.fin_audit_ai.domain.audit.entity.ShapFeatureImportanceEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiMetricCode;

import java.util.ArrayList;
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
        // 이 응답은 Redis에 캐시된다. Stream.toList()가 돌려주는 JDK 내부 불변 리스트는
        // 캐시 조회(역직렬화) 시 실패하므로 평범한 ArrayList로 감싸 캐시-안전하게 유지한다.
        List<XaiMetricResponse> metrics = new ArrayList<>(results.stream()
                .sorted(Comparator.comparingInt(
                        result -> metricOrder(result.getMetricCode())
                ))
                .map(XaiMetricResponse::from)
                .toList());

        return new ExplainabilityResponse(
                auditId,
                "SHAP",
                metrics,
                new ArrayList<>(topFeatures.stream()
                        .map(FeatureImportanceResponse::from)
                        .toList())
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