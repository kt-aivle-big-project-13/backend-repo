package com.aivle13.fin_audit_ai.domain.model.dto.response.model;

import java.time.LocalDateTime;

public record ModelUploadResponse(
        Long modelId,
        String modelName,
        String version,
        String fileName,
        LocalDateTime uploadedAt
) {
}
