package com.aivle13.fin_audit_ai.domain.audit.dto;

import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;

import java.util.List;

public record ExplainabilityResponseDto(
        Long auditId,
        String method,
        List<XaiMetricResponseDto> metrics
) {

    public static ExplainabilityResponseDto of(
            Long auditId,
            List<XaiResultEntity> results
    ) {
        List<XaiMetricResponseDto> metrics = results.stream()
                .map(XaiMetricResponseDto::from)
                .toList();

        return new ExplainabilityResponseDto(
                auditId,
                "SHAP",
                metrics
        );
    }
}