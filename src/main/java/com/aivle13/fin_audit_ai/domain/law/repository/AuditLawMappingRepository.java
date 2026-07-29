package com.aivle13.fin_audit_ai.domain.law.repository;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditLawMappingEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLawMappingRepository extends JpaRepository<AuditLawMappingEntity, Long> {

    List<AuditLawMappingEntity> findAllByAudit_Id(Long auditId);

    void deleteAllByAudit_IdAndCompliance(Long auditId, ComplianceStatus compliance);
}
