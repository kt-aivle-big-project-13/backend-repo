package com.aivle13.fin_audit_ai.domain.audit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuditStartRequest(
        @NotNull Long modelId,
        @NotNull Long datasetId,
        Long assessmentId,
        @NotBlank String auditName
) {
}
