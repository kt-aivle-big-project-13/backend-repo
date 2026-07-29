package com.aivle13.fin_audit_ai.domain.report.repository;

import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository
        extends JpaRepository<ReportEntity, Long> {
}