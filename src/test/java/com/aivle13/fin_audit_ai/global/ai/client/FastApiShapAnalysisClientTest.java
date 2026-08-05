package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.explainability.ExplainabilityResultRequest;
import com.aivle13.fin_audit_ai.global.ai.client.analysis.FastApiShapAnalysisClient;
import com.aivle13.fin_audit_ai.global.ai.dto.analysis.request.ShapAnalysisRequest;
import com.aivle13.fin_audit_ai.global.exception.ai.AiServerErrorException;
import com.aivle13.fin_audit_ai.global.exception.ai.AiServerTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FastApiShapAnalysisClientTest {

    private static final String BASE_URL = "http://localhost:8000";
    private static final String ANALYSIS_URL =
            BASE_URL + "/internal/v1/shap/analyze";

    @Test
    void returnsExplainabilityResultWhenAiServerSucceeds() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        FastApiShapAnalysisClient client =
                new FastApiShapAnalysisClient(
                        builder.baseUrl(BASE_URL).build()
                );

        server.expect(requestTo(ANALYSIS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "audit_id": 21,
                          "model_s3_key": "models/model.json",
                          "audit_dataset_s3_key": "datasets/audit.csv",
                          "target_column": "TARGET",
                          "sensitive_features": [
                            "CODE_GENDER",
                            "AGE_GROUP"
                          ]
                        }
                        """))
                .andRespond(withSuccess(
                        successResponse(),
                        MediaType.APPLICATION_JSON
                ));

        ExplainabilityResultRequest response =
                client.analyze(request());

        assertThat(response.pipelineStatus())
                .isEqualTo("COMPLETED");
        assertThat(response.overallStatus())
                .isEqualTo("WARNING");
        assertThat(
                response.keyMetrics()
                        .sensitiveContributionRatio()
                        .value()
        ).isEqualByComparingTo("0.0647");

        server.verify();
    }

    @Test
    void throwsEa001WhenAiServerReturnsError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        FastApiShapAnalysisClient client =
                new FastApiShapAnalysisClient(
                        builder.baseUrl(BASE_URL).build()
                );

        server.expect(requestTo(ANALYSIS_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.analyze(request()))
                .isInstanceOf(AiServerErrorException.class);

        server.verify();
    }

    @Test
    void throwsEa002WhenAiServerTimesOut() {
        RestClient timeoutRestClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory((uri, httpMethod) -> {
                    throw new SocketTimeoutException(
                            "AI server timeout"
                    );
                })
                .build();

        FastApiShapAnalysisClient client =
                new FastApiShapAnalysisClient(timeoutRestClient);

        assertThatThrownBy(() -> client.analyze(request()))
                .isInstanceOf(AiServerTimeoutException.class);
    }

    private ShapAnalysisRequest request() {
        return new ShapAnalysisRequest(
                21L,
                "models/model.json",
                "datasets/audit.csv",
                "TARGET",
                List.of("CODE_GENDER", "AGE_GROUP"),
                true,
                5
        );
    }

    private String successResponse() {
        return """
                {
                  "pipeline_status": "COMPLETED",
                  "overall_status": "WARNING",
                  "key_metrics": {
                    "sensitive_contribution_ratio": {
                      "metric": "SENSITIVE_CONTRIB",
                      "label": "민감변수 기여비율",
                      "value": 0.0647,
                      "threshold": 0.2,
                      "status": "PASS"
                    },
                    "global_explanation_stability": {
                      "metric": "GLOBAL_STABILITY",
                      "label": "전역 설명 안정성",
                      "value": 0.9996,
                      "threshold": 0.7,
                      "status": "PASS"
                    },
                    "explanation_fidelity": {
                      "metric": "FIDELITY",
                      "label": "설명 충실성",
                      "value": 0.4843,
                      "threshold": 0.5,
                      "status": "WARNING"
                    }
                  }
                }
                """;
    }
}