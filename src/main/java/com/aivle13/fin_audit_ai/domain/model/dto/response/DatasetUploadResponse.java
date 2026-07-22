package com.aivle13.fin_audit_ai.domain.model.dto.response;

import java.util.List;

public record DatasetUploadResponse(
        Long datasetId,
        Long modelId,
        String dataSource,
        int rowCount,
        List<String> columns
) {
}
