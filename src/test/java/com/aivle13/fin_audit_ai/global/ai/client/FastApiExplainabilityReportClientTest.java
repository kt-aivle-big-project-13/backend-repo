package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.global.ai.client.report.FastApiExplainabilityReportClient;
import com.aivle13.fin_audit_ai.global.ai.dto.report.request.ExplainabilityReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.ExplainabilityReportResponse;
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

class FastApiExplainabilityReportClientTest {

    private static final String BASE_URL = "http://localhost:8000";
    private static final String REPORT_URL =
            BASE_URL + "/internal/v1/reports/explainability";

    @Test
    void returnsReportResultWhenAiServerSucceeds() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        FastApiExplainabilityReportClient client =
                new FastApiExplainabilityReportClient(
                        builder.baseUrl(BASE_URL).build()
                );

        server.expect(requestTo(REPORT_URL))
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
                          ],
                          "report_top_n": 20
                        }
                        """))
                .andRespond(withSuccess(
                        successResponse(),
                        MediaType.APPLICATION_JSON
                ));

        ExplainabilityReportResponse response =
                client.generate(request());

        assertThat(response.auditId()).isEqualTo(21L);
        assertThat(response.reportS3Key())
                .isEqualTo(
                        "explainability-reports/21/run-123/report.html"
                );
        assertThat(response.pdfReportS3Key())
                .isEqualTo(
                        "explainability-reports/21/run-123/report.pdf"
                );
        assertThat(response.wordReportS3Key())
                .isEqualTo(
                        "explainability-reports/21/run-123/report.docx"
                );
        assertThat(response.format()).isEqualTo("html");
        assertThat(response.overallStatus())
                .isEqualTo("WARNING");
        assertThat(response.generatedAt())
                .isEqualTo("2026-07-29T10:00:00Z");

        server.verify();
    }

    @Test
    void throwsEa001WhenAiServerReturnsError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        FastApiExplainabilityReportClient client =
                new FastApiExplainabilityReportClient(
                        builder.baseUrl(BASE_URL).build()
                );

        server.expect(requestTo(REPORT_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.generate(request()))
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

        FastApiExplainabilityReportClient client =
                new FastApiExplainabilityReportClient(
                        timeoutRestClient
                );

        assertThatThrownBy(() -> client.generate(request()))
                .isInstanceOf(AiServerTimeoutException.class);
    }

    private ExplainabilityReportRequest request() {
        return new ExplainabilityReportRequest(
                21L,
                "models/model.json",
                "datasets/audit.csv",
                "TARGET",
                List.of("CODE_GENDER", "AGE_GROUP"),
                20
        );
    }

    private String successResponse() {
        return """
                {
                  "audit_id": 21,
                  "report_s3_key": "explainability-reports/21/run-123/report.html",
                  "pdf_report_s3_key": "explainability-reports/21/run-123/report.pdf",
                  "word_report_s3_key": "explainability-reports/21/run-123/report.docx",
                  "format": "html",
                  "overall_status": "WARNING",
                  "generated_at": "2026-07-29T10:00:00Z"
                }
                """;
    }
}
