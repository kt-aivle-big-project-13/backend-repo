package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.global.ai.dto.HighImpactReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.HighImpactReportResponse;
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

class FastApiHighImpactReportClientTest {

    private static final String BASE_URL =
            "http://localhost:8000";
    private static final String REPORT_URL =
            BASE_URL
                    + "/internal/v1/reports/high-impact-assessment";

    @Test
    void returnsReportResultWhenAiServerSucceeds() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        FastApiHighImpactReportClient client =
                new FastApiHighImpactReportClient(
                        builder.baseUrl(BASE_URL).build()
                );

        server.expect(requestTo(REPORT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "audit_id": 8,
                          "assessment_id": 50,
                          "audit_name": "신용평가 모델 감사",
                          "model_name": "신용평가 모델",
                          "model_version": "1.0",
                          "assessed_at": "2026-07-31T10:00:00+09:00",
                          "condition_met": true,
                          "group_a_score": 0,
                          "group_b_score": 0,
                          "total_score": 0,
                          "result": "HIGH_IMPACT",
                          "answers": [
                            {
                              "question_code": "GATE_01",
                              "question_text": "정성 게이트 문항 1",
                              "stage": "QUALITATIVE",
                              "group": "GATE",
                              "answer": true,
                              "weight": 0,
                              "score": 0
                            },
                            {
                              "question_code": "GATE_02",
                              "question_text": "정성 게이트 문항 2",
                              "stage": "QUALITATIVE",
                              "group": "GATE",
                              "answer": false,
                              "weight": 0,
                              "score": 0
                            }
                          ]
                        }
                        """))
                .andRespond(withSuccess(
                        successResponse(),
                        MediaType.APPLICATION_JSON
                ));

        HighImpactReportResponse response =
                client.generate(request());

        assertThat(response.auditId()).isEqualTo(8L);
        assertThat(response.assessmentId()).isEqualTo(50L);
        assertThat(response.pdfReportS3Key())
                .isEqualTo(
                        "high-impact-reports/8/50/run/report.pdf"
                );
        assertThat(response.wordReportS3Key())
                .isEqualTo(
                        "high-impact-reports/8/50/run/report.docx"
                );
        assertThat(response.generatedAt())
                .isEqualTo("2026-07-31T07:16:50Z");

        server.verify();
    }

    @Test
    void throwsEa001WhenAiServerReturnsError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        FastApiHighImpactReportClient client =
                new FastApiHighImpactReportClient(
                        builder.baseUrl(BASE_URL).build()
                );

        server.expect(requestTo(REPORT_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() ->
                client.generate(request())
        ).isInstanceOf(AiServerErrorException.class);

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

        FastApiHighImpactReportClient client =
                new FastApiHighImpactReportClient(
                        timeoutRestClient
                );

        assertThatThrownBy(() ->
                client.generate(request())
        ).isInstanceOf(AiServerTimeoutException.class);
    }

    private HighImpactReportRequest request() {
        return new HighImpactReportRequest(
                8L,
                50L,
                "신용평가 모델 감사",
                "신용평가 모델",
                "1.0",
                "2026-07-31T10:00:00+09:00",
                true,
                0,
                0,
                0,
                "HIGH_IMPACT",
                List.of(
                        new HighImpactReportRequest.Answer(
                                "GATE_01",
                                "정성 게이트 문항 1",
                                "QUALITATIVE",
                                "GATE",
                                true,
                                0,
                                0
                        ),
                        new HighImpactReportRequest.Answer(
                                "GATE_02",
                                "정성 게이트 문항 2",
                                "QUALITATIVE",
                                "GATE",
                                false,
                                0,
                                0
                        )
                )
        );
    }

    private String successResponse() {
        return """
                {
                  "audit_id": 8,
                  "assessment_id": 50,
                  "pdf_report_s3_key": "high-impact-reports/8/50/run/report.pdf",
                  "word_report_s3_key": "high-impact-reports/8/50/run/report.docx",
                  "generated_at": "2026-07-31T07:16:50Z"
                }
                """;
    }
}
