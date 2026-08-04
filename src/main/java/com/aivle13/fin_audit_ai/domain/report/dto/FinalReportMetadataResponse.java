package com.aivle13.fin_audit_ai.domain.report.dto;

import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportStatus;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;

import java.time.LocalDateTime;

/**
 * 최신 최종 감사 보고서 메타데이터.
 *
 * <p>필드 구성을 리포트 5종의 최신 조회 응답과 맞춘다. 클라이언트가 "이미 만들어진 산출물이
 * 있으면 생성 없이 내려받는다"는 같은 흐름을 종류에 상관없이 쓸 수 있어야 한다.
 */
public record FinalReportMetadataResponse(
        Long reportId,
        Long auditId,
        ReportType reportType,
        ReportFormat format,
        ReportStatus status,
        Integer version,
        LocalDateTime generatedAt
) {

    public static FinalReportMetadataResponse from(ReportEntity report) {
        return new FinalReportMetadataResponse(
                report.getId(),
                report.getAudit().getId(),
                report.getReportType(),
                report.getFormat(),
                report.getStatus(),
                report.getVersion(),
                report.getCreatedAt()
        );
    }
}
