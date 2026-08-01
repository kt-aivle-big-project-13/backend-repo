package com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessGroupStatEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessMetricCode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record FairnessResultResponse(
        Long auditId,
        String method,
        List<FairnessMetricResponse> results,
        PerformanceResponse performance,
        List<FairnessGroupStatResponse> groupStats
) {

    // 지표만 있는 응답(성능·집단통계 없이). 캐싱 위임 등 내부 용도.
    public static FairnessResultResponse of(
            Long auditId,
            List<FairnessResultEntity> results
    ) {
        return of(auditId, results, new ArrayList<>(), null);
    }

    public static FairnessResultResponse of(
            Long auditId,
            List<FairnessResultEntity> results,
            List<FairnessGroupStatEntity> groupStats,
            AuditEntity audit
    ) {
        // 이 응답은 Redis에 캐시된다. Stream.toList()가 돌려주는 JDK 내부 불변 리스트는
        // 캐시 조회(역직렬화) 시 실패하므로 평범한 ArrayList로 감싸 캐시-안전하게 유지한다.
        List<FairnessMetricResponse> sortedResults = new ArrayList<>(results.stream()
                .sorted(Comparator
                        .comparing(FairnessResultEntity::getAttribute)
                        .thenComparingInt(result -> metricOrder(result.getMetricCode())))
                .map(FairnessMetricResponse::from)
                .toList());

        List<FairnessGroupStatResponse> sortedStats = new ArrayList<>(groupStats.stream()
                .sorted(Comparator
                        .comparing(FairnessGroupStatEntity::getAttribute)
                        .thenComparing(FairnessGroupStatEntity::getGroupName))
                .map(FairnessGroupStatResponse::from)
                .toList());

        return new FairnessResultResponse(
                auditId, "FAIRLEARN", sortedResults, PerformanceResponse.from(audit), sortedStats);
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
        };
    }
}
