package com.aivle13.fin_audit_ai.domain.audit.dto.response.core;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;

import java.time.LocalDateTime;

public record AuditRetryResponse(
        Long auditId,
        String status,
        LocalDateTime retriedAt
) {
    public static AuditRetryResponse from(AuditEntity audit, LocalDateTime retriedAt) {
        return new AuditRetryResponse(audit.getId(), audit.getStatus().name(), retriedAt);
    }
}
