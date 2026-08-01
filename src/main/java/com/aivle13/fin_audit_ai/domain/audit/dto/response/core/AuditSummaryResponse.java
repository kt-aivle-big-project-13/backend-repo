package com.aivle13.fin_audit_ai.domain.audit.dto.response.core;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;

import java.time.LocalDateTime;

public record AuditSummaryResponse(
        Long auditId,
        String modelName,
        String modelFileName,
        String datasetFileName,
        String modelGroupId,
        String version,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        AuditStatus status,
        int currentStep
) {
    public static AuditSummaryResponse from(AuditEntity audit) {
        return new AuditSummaryResponse(
                audit.getId(),
                audit.getModel().getModelName(),
                audit.getModel().getOriginalFileName(),
                audit.getDataset().getOriginalFileName(),
                audit.getModel().getModelGroupId(),
                audit.getModel().getVersion(),
                audit.getCreatedAt(),
                audit.getCompletedAt(),
                audit.getStatus(),
                audit.getCurrentStep()
        );
    }
}
