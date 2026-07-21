package com.aivle13.fin_audit_ai.domain.model.dto;

import java.util.List;

public record DatasetUploadResponseDto(
        Long datasetId,
        Long modelId,
        String dataSource,
        int rowCount,
        List<String> columns
) {
}
