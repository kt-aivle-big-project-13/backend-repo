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
}