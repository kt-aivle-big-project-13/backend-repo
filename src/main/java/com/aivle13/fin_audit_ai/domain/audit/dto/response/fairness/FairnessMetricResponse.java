package com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness;

import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessStatus;

import java.math.BigDecimal;

public record FairnessMetricResponse(
        String attribute,
        FairnessMetricCode metricCode,
        BigDecimal value,
        BigDecimal threshold,
        FairnessStatus status
) {

    public static FairnessMetricResponse from(FairnessResultEntity result) {
        return new FairnessMetricResponse(
                result.getAttribute(),
                result.getMetricCode(),
                result.getValue(),
                result.getThreshold(),
                result.getStatus()
        );
    }
}
