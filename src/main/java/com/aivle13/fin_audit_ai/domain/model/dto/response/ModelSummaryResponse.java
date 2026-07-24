package com.aivle13.fin_audit_ai.domain.model.dto.response;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;

import java.time.LocalDateTime;

public record ModelSummaryResponse(
        Long modelId,
        String modelName,
        String currentVersion,
        Boolean highImpact,
        LocalDateTime uploadedAt
) {
    public static ModelSummaryResponse from(AiModelEntity model) {
        return new ModelSummaryResponse(
                model.getId(),
                model.getModelName(),
                model.getVersion(),
                model.getHighImpact(),
                model.getCreatedAt()
        );
    }
}
