package com.aivle13.fin_audit_ai.domain.audit.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ExplainabilityResultRequestDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesSnakeCaseJsonIntoRecord() throws Exception {
        String json = """
                {
                  "pipeline_status": "COMPLETED",
                  "overall_status": "WARNING",
                  "key_metrics": {
                    "sensitive_contribution_ratio": {
                      "metric": "sensitive_contribution_ratio",
                      "label": "민감변수 기여비율",
                      "value": 0.0647,
                      "threshold": 0.2000,
                      "status": "PASS"
                    },
                    "global_explanation_stability": {
                      "metric": "global_explanation_stability",
                      "label": "전역 설명 안정성",
                      "value": 0.9996,
                      "threshold": 0.7000,
                      "status": "PASS"
                    },
                    "explanation_fidelity": {
                      "metric": "explanation_fidelity",
                      "label": "설명 충실성",
                      "value": 0.4843,
                      "threshold": 0.5000,
                      "status": "WARNING"
                    }
                  }
                }
                """;

        ExplainabilityResultRequestDto request =
                objectMapper.readValue(json, ExplainabilityResultRequestDto.class);

        assertThat(request.pipelineStatus()).isEqualTo("COMPLETED");
        assertThat(request.overallStatus()).isEqualTo("WARNING");

        ExplainabilityResultRequestDto.KeyMetrics metrics = request.keyMetrics();
        assertThat(metrics.sensitiveContributionRatio().value())
                .isEqualTo(new BigDecimal("0.0647"));
        assertThat(metrics.sensitiveContributionRatio().status())
                .isEqualTo("PASS");
        assertThat(metrics.globalExplanationStability().threshold())
                .isEqualTo(new BigDecimal("0.7000"));
        assertThat(metrics.explanationFidelity().status())
                .isEqualTo("WARNING");
    }

    @Test
    void deserializesMissingKeyMetricsAsNull() throws Exception {
        String json = """
                {
                  "pipeline_status": "FAILED",
                  "overall_status": "NON_COMPLIANT"
                }
                """;

        ExplainabilityResultRequestDto request =
                objectMapper.readValue(json, ExplainabilityResultRequestDto.class);

        assertThat(request.pipelineStatus()).isEqualTo("FAILED");
        assertThat(request.overallStatus()).isEqualTo("NON_COMPLIANT");
        assertThat(request.keyMetrics()).isNull();
    }

    @Test
    void deserializesMissingMetricFieldsAsNull() throws Exception {
        String json = """
                {
                  "pipeline_status": "COMPLETED",
                  "overall_status": "COMPLIANT",
                  "key_metrics": {
                    "sensitive_contribution_ratio": {
                      "metric": "sensitive_contribution_ratio",
                      "status": "PASS"
                    }
                  }
                }
                """;

        ExplainabilityResultRequestDto request =
                objectMapper.readValue(json, ExplainabilityResultRequestDto.class);

        ExplainabilityResultRequestDto.Metric metric =
                request.keyMetrics().sensitiveContributionRatio();

        assertThat(metric.value()).isNull();
        assertThat(metric.threshold()).isNull();
        assertThat(metric.status()).isEqualTo("PASS");
        assertThat(request.keyMetrics().globalExplanationStability()).isNull();
    }
}