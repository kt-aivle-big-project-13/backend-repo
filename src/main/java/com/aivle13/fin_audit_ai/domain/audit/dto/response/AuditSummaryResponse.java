package com.aivle13.fin_audit_ai.domain.audit.dto.response;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;

import java.time.LocalDateTime;

public record AuditSummaryResponse(
        Long auditId,
        String modelName,
        LocalDateTime completedAt,
        AuditStatus status,
        int currentStep
) {
    public static AuditSummaryResponse from(AuditEntity audit) {
        return new AuditSummaryResponse(
                audit.getId(),
                audit.getModel().getModelName(),
                audit.getCompletedAt(),
                audit.getStatus(),
                audit.getCurrentStep()
        );
    }
}
