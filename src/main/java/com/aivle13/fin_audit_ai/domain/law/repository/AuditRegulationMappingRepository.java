package com.aivle13.fin_audit_ai.domain.law.repository;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditRegulationMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRegulationMappingRepository extends JpaRepository<AuditRegulationMappingEntity, Long> {

    List<AuditRegulationMappingEntity> findAllByAudit_Id(Long auditId);

    void deleteAllByAudit_Id(Long auditId);
}
