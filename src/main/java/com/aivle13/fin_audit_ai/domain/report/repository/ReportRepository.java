package com.aivle13.fin_audit_ai.domain.report.repository;

import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository
        extends JpaRepository<ReportEntity, Long> {

    Optional<ReportEntity>
    findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
            Long auditId,
            Long userId,
            ReportType reportType,
            ReportFormat format
    );

    Optional<ReportEntity>
    findByIdAndAudit_IdAndAudit_User_IdAndReportType(
            Long reportId,
            Long auditId,
            Long userId,
            ReportType reportType
    );

    // 최종 감사 보고서 다운로드 경로에는 감사 id 가 없으므로 리포트 id 와 소유자로만 좁힌다.
    Optional<ReportEntity> findByIdAndAudit_User_IdAndReportType(
            Long reportId,
            Long userId,
            ReportType reportType
    );
}