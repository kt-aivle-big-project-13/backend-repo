package com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness;

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

    // enum 값을 전부 다뤄 컴파일러가 누락을 잡아주도록 default 없이 둔다 — 새 지표를
    // 추가하고 여기 안 채우면 컴파일이 깨진다.
    private static int metricOrder(FairnessMetricCode metricCode) {
        return switch (metricCode) {
            case DEMOGRAPHIC_PARITY -> 0;
            case PROPORTIONAL_PARITY -> 1;
            case EQUAL_OPPORTUNITY -> 2;
            case EQUALIZED_ODDS -> 3;
            case FPR_PARITY -> 4;
            case FDR_PARITY -> 5;
            case FOR_PARITY -> 6;
            case FNR_PARITY -> 7;
        };
    }
}
