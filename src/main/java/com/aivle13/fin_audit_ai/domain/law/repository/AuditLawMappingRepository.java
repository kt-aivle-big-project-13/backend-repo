package com.aivle13.fin_audit_ai.domain.law.repository;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditLawMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLawMappingRepository extends JpaRepository<AuditLawMappingEntity, Long> {

    List<AuditLawMappingEntity> findAllByAudit_Id(Long auditId);

    void deleteAllByAudit_Id(Long auditId);
}
