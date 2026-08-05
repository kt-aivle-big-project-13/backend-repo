package com.aivle13.fin_audit_ai.global.ai.dto;

import com.aivle13.fin_audit_ai.global.ai.dto.report.request.HighImpactReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.HighImpactReportResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HighImpactReportDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesRequestUsingAiServerFieldNames() throws Exception {
        HighImpactReportRequest request =
                new HighImpactReportRequest(
                        8L,
                        50L,
                        "신용평가 모델 감사",
                        "신용평가 모델",
                        "1.0",
                        "2026-07-31T10:00:00+09:00",
                        false,
                        4,
                        0,
                        4,
                        "HIGH_IMPACT",
                        List.of(
                                new HighImpactReportRequest.Answer(
                                        "A_01",
                                        "신규 AI 모델인가요?",
                                        "QUANTITATIVE",
                                        "A",
                                        true,
                                        2,
                                        2
                                )
                        )
                );

        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(request)
        );

        assertThat(json.get("audit_id").asLong())
                .isEqualTo(8L);
        assertThat(json.get("assessment_id").asLong())
                .isEqualTo(50L);
        assertThat(json.get("audit_name").asText())
                .isEqualTo("신용평가 모델 감사");
        assertThat(json.get("model_name").asText())
                .isEqualTo("신용평가 모델");
        assertThat(json.get("model_version").asText())
                .isEqualTo("1.0");
        assertThat(json.get("assessed_at").asText())
                .isEqualTo("2026-07-31T10:00:00+09:00");
        assertThat(json.get("condition_met").asBoolean())
                .isFalse();
        assertThat(json.get("group_a_score").asInt())
                .isEqualTo(4);
        assertThat(json.get("group_b_score").asInt())
                .isZero();
        assertThat(json.get("total_score").asInt())
                .isEqualTo(4);

        JsonNode answer = json.get("answers").get(0);

        assertThat(answer.get("question_code").asText())
                .isEqualTo("A_01");
        assertThat(answer.get("question_text").asText())
                .isEqualTo("신규 AI 모델인가요?");
        assertThat(answer.get("stage").asText())
                .isEqualTo("QUANTITATIVE");
        assertThat(answer.get("group").asText())
                .isEqualTo("A");
        assertThat(answer.get("weight").asInt())
                .isEqualTo(2);
        assertThat(answer.get("score").asInt())
                .isEqualTo(2);

        assertThat(json.has("auditId")).isFalse();
        assertThat(json.has("assessmentId")).isFalse();
    }

    @Test
    void deserializesAiServerResponse() throws Exception {
        String json = """
                {
                  "audit_id": 8,
                  "assessment_id": 50,
                  "pdf_report_s3_key": "reports/run/report.pdf",
                  "word_report_s3_key": "reports/run/report.docx",
                  "generated_at": "2026-07-31T07:16:50Z"
                }
                """;

        HighImpactReportResponse response =
                objectMapper.readValue(
                        json,
                        HighImpactReportResponse.class
                );

        assertThat(response.auditId()).isEqualTo(8L);
        assertThat(response.assessmentId()).isEqualTo(50L);
        assertThat(response.pdfReportS3Key())
                .isEqualTo("reports/run/report.pdf");
        assertThat(response.wordReportS3Key())
                .isEqualTo("reports/run/report.docx");
        assertThat(response.generatedAt())
                .isEqualTo("2026-07-31T07:16:50Z");
    }
}
