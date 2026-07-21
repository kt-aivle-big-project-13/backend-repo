package com.aivle13.fin_audit_ai.domain.model.dto;

import java.time.LocalDateTime;

public record ModelUploadResponseDto(
        Long modelId,
        String modelName,
        String version,
        String fileName,
        LocalDateTime uploadedAt
) {
}
