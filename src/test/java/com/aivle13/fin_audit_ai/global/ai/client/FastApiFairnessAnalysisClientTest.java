package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.fairness.FairnessRunRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness.FairnessRunResponse;
import com.aivle13.fin_audit_ai.global.exception.ai.AiServerErrorException;
import com.aivle13.fin_audit_ai.global.exception.ai.AiServerTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FastApiFairnessAnalysisClientTest {

    private static final String BASE_URL = "http://localhost:8000";
    private static final String ANALYSIS_URL =
            BASE_URL + "/internal/v1/fairness/analyze";

    @Test
    void returnsFairnessRunResponseWhenAiServerSucceeds() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        FastApiFairnessAnalysisClient client =
                new FastApiFairnessAnalysisClient(
                        builder.baseUrl(BASE_URL).build()
                );

        server.expect(requestTo(ANALYSIS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "audit_id": 21,
                          "model_s3_key": "models/model.json",
                          "audit_dataset_s3_key": "datasets/audit.csv",
                          "validation_dataset_s3_key": "datasets/valid.csv",
                          "audit_name": "테스트 감사",
                          "target_approval_rate": 0.9,
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

        FairnessRunResponse response = client.analyze(request());

        assertThat(response.auditId()).isEqualTo("21");
        assertThat(response.approvalRate())
                .isEqualByComparingTo("0.91");
        assertThat(
                response.fairnessByAttribute()
                        .get("CODE_GENDER")
                        .demographicParityDifference()
        ).isEqualByComparingTo("0.05");

        server.verify();
    }

    @Test
    void throwsEa001WhenAiServerReturnsError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        FastApiFairnessAnalysisClient client =
                new FastApiFairnessAnalysisClient(
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

        FastApiFairnessAnalysisClient client =
                new FastApiFairnessAnalysisClient(timeoutRestClient);

        assertThatThrownBy(() -> client.analyze(request()))
                .isInstanceOf(AiServerTimeoutException.class);
    }

    private FairnessRunRequest request() {
        return new FairnessRunRequest(
                21L,
                "models/model.json",
                "datasets/audit.csv",
                "datasets/valid.csv",
                "테스트 감사",
                new BigDecimal("0.9"),
                null,
                "CODE_GENDER, AGE_GROUP"
        );
    }

    private String successResponse() {
        return """
                {
                  "audit_id": "21",
                  "audit_name": "테스트 감사",
                  "threshold": {
                    "value": 0.9,
                    "method": "target_approval_rate",
                    "basis": "validation",
                    "computed_from": "valid_processed.csv"
                  },
                  "n_customers": 1000,
                  "approval_rate": 0.91,
                  "calibration_source": "validation_dataset",
                  "fairness_by_attribute": {
                    "CODE_GENDER": {
                      "attribute": "CODE_GENDER",
                      "status": "REVIEW",
                      "demographic_parity_difference": 0.05,
                      "equal_opportunity_difference": 0.03,
                      "equalized_odds_difference": 0.04,
                      "groups": [],
                      "excluded_groups": [],
                      "note": null
                    }
                  },
                  "fairness_summary": {},
                  "warnings": []
                }
                """;
    }
}
