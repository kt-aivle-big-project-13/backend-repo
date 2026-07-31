package com.aivle13.fin_audit_ai.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BiasReportResponse(

        @JsonProperty("audit_id")
        Long auditId,

        @JsonProperty("report_s3_key")
        String reportS3Key,

        @JsonProperty("pdf_report_s3_key")
        String pdfReportS3Key,

        String format,

        @JsonProperty("generated_at")
        String generatedAt
) {
}
