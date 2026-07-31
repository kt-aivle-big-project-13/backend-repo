package com.aivle13.fin_audit_ai.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ComplianceReportResponse(

        @JsonProperty("audit_id")
        Long auditId,

        @JsonProperty("report_s3_key")
        String reportS3Key,

        @JsonProperty("pdf_report_s3_key")
        String pdfReportS3Key,

        @JsonProperty("word_report_s3_key")
        String wordReportS3Key,

        String format,

        @JsonProperty("compliant_count")
        Integer compliantCount,

        @JsonProperty("non_compliant_count")
        Integer nonCompliantCount,

        @JsonProperty("pending_count")
        Integer pendingCount,

        @JsonProperty("generated_at")
        String generatedAt
) {
}
