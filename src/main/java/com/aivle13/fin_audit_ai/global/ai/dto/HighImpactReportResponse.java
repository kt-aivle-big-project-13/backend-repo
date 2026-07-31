package com.aivle13.fin_audit_ai.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HighImpactReportResponse(

        @JsonProperty("audit_id")
        Long auditId,

        @JsonProperty("assessment_id")
        Long assessmentId,

        @JsonProperty("pdf_report_s3_key")
        String pdfReportS3Key,

        @JsonProperty("word_report_s3_key")
        String wordReportS3Key,

        @JsonProperty("generated_at")
        String generatedAt
) {
}
