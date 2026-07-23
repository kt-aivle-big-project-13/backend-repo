package com.aivle13.fin_audit_ai.domain.audit.dto.response;

import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessMetricCode;

import java.util.Comparator;
import java.util.List;

public record FairnessResultResponse(
        Long auditId,
        String method,
        List<FairnessMetricResponse> results
) {

    public static FairnessResultResponse of(
            Long auditId,
            List<FairnessResultEntity> results
    ) {
        List<FairnessMetricResponse> sorted = results.stream()
                .sorted(Comparator
                        .comparing(FairnessResultEntity::getAttribute)
                        .thenComparingInt(result -> metricOrder(result.getMetricCode())))
                .map(FairnessMetricResponse::from)
                .toList();

        return new FairnessResultResponse(auditId, "FAIRLEARN", sorted);
    }

    private static int metricOrder(FairnessMetricCode metricCode) {
        return switch (metricCode) {
            case DEMOGRAPHIC_PARITY -> 0;
            case EQUAL_OPPORTUNITY -> 1;
            case EQUALIZED_ODDS -> 2;
            default -> Integer.MAX_VALUE;
        };
    }
}
