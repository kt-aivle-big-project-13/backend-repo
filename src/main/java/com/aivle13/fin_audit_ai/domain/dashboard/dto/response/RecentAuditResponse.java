package com.aivle13.fin_audit_ai.domain.dashboard.dto.response;

import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;

import java.time.LocalDateTime;

public record RecentAuditResponse(
        Long auditId,
        Long modelId,
        String modelName,
        String version,
        AuditStatus status,
        String keyRisk,
        LocalDateTime completedAt
) {
}
