package com.aivle13.fin_audit_ai.domain.audit.dto.request.explainability;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 서버(ai-repo)가 include_report=true 로 실제 내려주는 것과 동일한 모양의 JSON을
 * 그대로 역직렬화해서, report.global_importance_top 매핑과 미모델링 필드 무시가
 * 실제로 동작하는지 확인한다 (Spring 컨텍스트 없이 순수 Jackson 매핑만 검증).
 */
class ExplainabilityResultRequestJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesGlobalImportanceTopFromRealisticAiResponse() throws Exception {
        String json = """
                {
                  "pipeline_status": "COMPLETED",
                  "overall_status": "PASS",
                  "key_metrics": {
                    "sensitive_contribution_ratio": {
                      "metric": "SENSITIVE_CONTRIB", "label": "민감변수 기여비율",
                      "value": 0.0647, "threshold": 0.2000, "status": "PASS"
                    },
                    "global_explanation_stability": {
                      "metric": "GLOBAL_STABILITY", "label": "설명 일관성",
                      "value": 0.9996, "threshold": 0.7000, "status": "PASS"
                    },
                    "explanation_fidelity": {
                      "metric": "FIDELITY", "label": "설명 충실성",
                      "value": 0.4843, "threshold": 0.5000, "status": "REVIEW"
                    }
                  },
                  "report": {
                    "schema_validation": { "status": "OK", "row_count": 1000 },
                    "metrics": [ { "metric": "FIDELITY", "value": 0.4843 } ],
                    "global_importance_top": [
                      {
                        "rank": 1, "feature": "EXT_SOURCE_2",
                        "mean_abs_shap": 0.5123, "mean_signed_shap": -0.3011,
                        "contribution_ratio": 0.191, "direction": "RISK_DECREASE",
                        "is_sensitive": false, "sensitive_group": null,
                        "unexpected_future_field": "should be ignored"
                      },
                      {
                        "rank": 4, "feature": "CODE_GENDER",
                        "mean_abs_shap": 0.1200, "mean_signed_shap": 0.0400,
                        "contribution_ratio": 0.086, "direction": "RISK_INCREASE",
                        "is_sensitive": true, "sensitive_group": "CODE_GENDER"
                      }
                    ],
                    "sampling": { "sample_size": 5000.0 },
                    "thresholds": { "sensitive_contrib": 0.2 },
                    "manifest": { "model": "credit_model.json" },
                    "limitations": ["프록시 효과는 측정하지 않음"]
                  },
                  "artifacts": { "bucket": "some-bucket", "prefix": "x", "files": [] },
                  "artifacts_status": "UPLOADED"
                }
                """;

        ExplainabilityResultRequest result =
                objectMapper.readValue(json, ExplainabilityResultRequest.class);

        assertThat(result.pipelineStatus()).isEqualTo("COMPLETED");
        assertThat(result.report()).isNotNull();

        var topFeatures = result.report().globalImportanceTop();
        assertThat(topFeatures).hasSize(2);

        var first = topFeatures.get(0);
        assertThat(first.rank()).isEqualTo(1);
        assertThat(first.feature()).isEqualTo("EXT_SOURCE_2");
        assertThat(first.contributionRatio()).isEqualByComparingTo("0.191");
        assertThat(first.isSensitive()).isFalse();

        var second = topFeatures.get(1);
        assertThat(second.feature()).isEqualTo("CODE_GENDER");
        assertThat(second.isSensitive()).isTrue();
        assertThat(second.sensitiveGroup()).isEqualTo("CODE_GENDER");
    }

    @Test
    void reportIsNullWhenAiOmitsIt() throws Exception {
        // include_report=false(기본값)일 때 AI가 실제로 보내는 형태 — report 필드 자체가 없음
        String json = """
                {
                  "pipeline_status": "COMPLETED",
                  "overall_status": "PASS",
                  "key_metrics": {
                    "sensitive_contribution_ratio": {"metric": "SENSITIVE_CONTRIB", "label": "x", "value": 0.06, "threshold": 0.2, "status": "PASS"},
                    "global_explanation_stability": {"metric": "GLOBAL_STABILITY", "label": "x", "value": 0.99, "threshold": 0.7, "status": "PASS"},
                    "explanation_fidelity": {"metric": "FIDELITY", "label": "x", "value": 0.48, "threshold": 0.5, "status": "REVIEW"}
                  }
                }
                """;

        ExplainabilityResultRequest result =
                objectMapper.readValue(json, ExplainabilityResultRequest.class);

        assertThat(result.report()).isNull();
    }
}
