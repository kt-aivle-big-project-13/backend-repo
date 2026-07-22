package com.aivle13.fin_audit_ai.domain.audit.dto.request;

import com.aivle13.fin_audit_ai.domain.audit.type.ThresholdMethod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AuditStartRequest(
        @NotNull Long modelId,
        @NotNull Long datasetId,
        Long assessmentId,
        @NotBlank String auditName,
        @NotNull ThresholdMethod thresholdMethod,

        // thresholdMethod=VALIDATION_DATASET일 때만 사용
        @DecimalMin(value = "0", inclusive = false) @DecimalMax(value = "1", inclusive = false)
        BigDecimal targetApprovalRate,

        // thresholdMethod=MANUAL일 때만 사용
        @DecimalMin("0") @DecimalMax("1")
        BigDecimal manualThreshold
) {
}
