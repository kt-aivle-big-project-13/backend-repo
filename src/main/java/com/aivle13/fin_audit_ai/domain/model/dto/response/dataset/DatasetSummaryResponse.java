package com.aivle13.fin_audit_ai.domain.model.dto.response.dataset;

import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record DatasetSummaryResponse(
        Long datasetId,
        Long modelId,
        String modelVersion,
        String purpose,
        List<String> columns,
        boolean audited,
        LocalDateTime createdAt
) {
    public static DatasetSummaryResponse from(DatasetEntity dataset) {
        return new DatasetSummaryResponse(
                dataset.getId(),
                dataset.getModel().getId(),
                dataset.getModel().getVersion(),
                dataset.getPurpose().name(),
                splitToArrayList(dataset.getColumns()),
                dataset.isAudited(),
                dataset.getCreatedAt()
        );
    }

    // 이 응답은 Redis에 캐시된다. List.of()/Stream.toList()가 돌려주는 JDK 내부 불변 리스트
    // (ImmutableCollections$ListN 등)는 캐시 조회 시 역직렬화가 실패해 두 번째 요청부터
    // 500 에러가 나므로, 반드시 평범한 ArrayList로 만들어 캐시-안전하게 유지한다.
    private static List<String> splitToArrayList(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.stream(commaSeparated.split(",")).map(String::trim).toList());
    }
}
