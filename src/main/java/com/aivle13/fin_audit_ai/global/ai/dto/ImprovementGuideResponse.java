package com.aivle13.fin_audit_ai.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ImprovementGuideResponse(

        @JsonProperty("audit_id")
        Long auditId,

        @JsonProperty("report_s3_key")
        String reportS3Key,

        @JsonProperty("pdf_report_s3_key")
        String pdfReportS3Key,

        @JsonProperty("word_report_s3_key")
        String wordReportS3Key,

        String format,

        @JsonProperty("high_priority_count")
        Integer highPriorityCount,

        @JsonProperty("medium_priority_count")
        Integer mediumPriorityCount,

        @JsonProperty("low_priority_count")
        Integer lowPriorityCount,

        @JsonProperty("generated_at")
        String generatedAt
) {
}
