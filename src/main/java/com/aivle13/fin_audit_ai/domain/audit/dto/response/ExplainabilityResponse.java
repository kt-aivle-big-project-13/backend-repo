package com.aivle13.fin_audit_ai.domain.audit.dto.response;

import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;

import java.util.List;

public record ExplainabilityResponse(
        Long auditId,
        String method,
        List<XaiMetricResponse> metrics
) {

    public static ExplainabilityResponse of(
            Long auditId,
            List<XaiResultEntity> results
    ) {
        List<XaiMetricResponse> metrics = results.stream()
                .map(XaiMetricResponse::from)
                .toList();

        return new ExplainabilityResponse(
                auditId,
                "SHAP",
                metrics
        );
    }
}