package com.aivle13.fin_audit_ai.domain.audit.dto;

import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExplainabilityResponseDtoTest {

    private static final Long AUDIT_ID = 21L;

    @Test
    void mapsAuditIdMethodAndMetricsFromResults() {
        List<XaiResultEntity> results = List.of(
                XaiResultEntity.of(
                        null,
                        XaiMetricCode.SENSITIVE_CONTRIB,
                        new BigDecimal("0.0647"),
                        new BigDecimal("0.2000"),
                        XaiStatus.PASS
                ),
                XaiResultEntity.of(
                        null,
                        XaiMetricCode.FIDELITY,
                        new BigDecimal("0.4843"),
                        new BigDecimal("0.5000"),
                        XaiStatus.REVIEW
                )
        );

        ExplainabilityResponseDto response =
                ExplainabilityResponseDto.of(AUDIT_ID, results);

        assertThat(response.auditId()).isEqualTo(AUDIT_ID);
        assertThat(response.method()).isEqualTo("SHAP");
        assertThat(response.metrics())
                .extracting(XaiMetricResponseDto::metricCode)
                .containsExactly(
                        XaiMetricCode.SENSITIVE_CONTRIB,
                        XaiMetricCode.FIDELITY
                );
    }

    @Test
    void returnsEmptyMetricsWhenResultsIsEmpty() {
        ExplainabilityResponseDto response =
                ExplainabilityResponseDto.of(AUDIT_ID, List.of());

        assertThat(response.auditId()).isEqualTo(AUDIT_ID);
        assertThat(response.method()).isEqualTo("SHAP");
        assertThat(response.metrics()).isEmpty();
    }
}