package com.aivle13.fin_audit_ai.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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
        String generatedAt,

        // 챗봇이 리포트 내용을 근거로 답할 수 있도록 섹션별 서술을 함께 받는다.
        // 구버전 AI 서버는 내려주지 않으므로 비어 있을 수 있다.
        List<ReportNarrativeResponse> narratives
) {
}
