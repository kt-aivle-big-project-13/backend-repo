package com.aivle13.fin_audit_ai.domain.dashboard.dto.response;

import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;

public record ReviewRequiredModelResponse(
        Long modelId,
        String modelName,
        String version,
        long issueCount,
        AuditStatus status
) {
}
