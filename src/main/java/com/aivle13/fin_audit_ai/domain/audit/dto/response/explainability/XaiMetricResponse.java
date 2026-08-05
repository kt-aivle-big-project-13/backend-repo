package com.aivle13.fin_audit_ai.domain.audit.dto.response.explainability;

import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiStatus;

import java.math.BigDecimal;

public record XaiMetricResponse(
        XaiMetricCode metricCode,
        BigDecimal value,
        BigDecimal threshold,
        XaiStatus status
) {

    public static XaiMetricResponse from(XaiResultEntity result) {
        return new XaiMetricResponse(
                result.getMetricCode(),
                result.getValue(),
                result.getThreshold(),
                result.getStatus()
        );
    }
}