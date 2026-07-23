package com.aivle13.fin_audit_ai.domain.model.dto.response;

import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;

import java.time.LocalDateTime;
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
                Arrays.stream(dataset.getColumns().split(",")).map(String::trim).toList(),
                dataset.isAudited(),
                dataset.getCreatedAt()
        );
    }
}
