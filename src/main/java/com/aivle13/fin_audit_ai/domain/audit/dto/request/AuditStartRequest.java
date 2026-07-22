package com.aivle13.fin_audit_ai.domain.audit.dto.request;

import com.aivle13.fin_audit_ai.domain.audit.type.ThresholdMethod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AuditStartRequest(
        @NotNull Long modelId,
        @NotNull Long datasetId,
        Long assessmentId,
        @NotBlank String auditName,
        @NotNull ThresholdMethod thresholdMethod,

        // thresholdMethod=VALIDATION_DATASET일 때만 사용. AuditEntity.targetApprovalRate 컬럼(precision=5, scale=4)과 정밀도를 맞춘다.
        @DecimalMin(value = "0", inclusive = false) @DecimalMax(value = "1", inclusive = false)
        @Digits(integer = 1, fraction = 4)
        BigDecimal targetApprovalRate,

        // thresholdMethod=MANUAL일 때만 사용. AuditEntity.manualThreshold 컬럼(precision=5, scale=4)과 정밀도를 맞춘다.
        @DecimalMin("0") @DecimalMax("1")
        @Digits(integer = 1, fraction = 4)
        BigDecimal manualThreshold
) {
}
