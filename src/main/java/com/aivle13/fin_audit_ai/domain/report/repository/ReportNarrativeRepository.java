package com.aivle13.fin_audit_ai.domain.report.repository;

import com.aivle13.fin_audit_ai.domain.report.entity.ReportNarrativeEntity;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportNarrativeRepository
        extends JpaRepository<ReportNarrativeEntity, Long> {

    void deleteByAudit_IdAndReportType(Long auditId, ReportType reportType);

    // 챗봇 프롬프트에 넣을 때 리포트 목차와 같은 순서를 유지한다.
    List<ReportNarrativeEntity> findAllByAudit_IdOrderByReportTypeAscDisplayOrderAsc(
            Long auditId
    );
}
