package com.aivle13.fin_audit_ai.domain.report.dto;

import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportStatus;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;

public record GeneratedReportResponse(
        Long reportId,
        ReportType reportType,
        ReportFormat format,
        ReportStatus status
) {

    // 생성이 완료된 최종 감사 보고서 응답 생성
    public static GeneratedReportResponse completed(
            Long reportId,
            ReportFormat format
    ) {
        return new GeneratedReportResponse(
                reportId,
                ReportType.FINAL_AUDIT_REPORT,
                format,
                ReportStatus.COMPLETED
        );
    }
}