package com.aivle13.fin_audit_ai.domain.audit.dto;

import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiStatus;

import java.math.BigDecimal;

public record XaiMetricResponseDto(
        XaiMetricCode metricCode,
        BigDecimal value,
        BigDecimal threshold,
        XaiStatus status
) {

    public static XaiMetricResponseDto from(XaiResultEntity result) {
        return new XaiMetricResponseDto(
                result.getMetricCode(),
                result.getValue(),
                result.getThreshold(),
                result.getStatus()
        );
    }
}