package com.aivle13.fin_audit_ai.domain.audit.dto.response;

import java.time.LocalDateTime;

public record AuditStartResponse(
        Long auditId,
        String status,
        LocalDateTime startedAt
) {
}
