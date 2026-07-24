package com.aivle13.fin_audit_ai.domain.audit.dto.response.core;

import java.time.LocalDateTime;

public record AuditStartResponse(
        Long auditId,
        String status,
        LocalDateTime startedAt
) {
}
