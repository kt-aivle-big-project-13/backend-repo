package com.aivle13.fin_audit_ai.domain.audit.repository;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditFileRepository extends JpaRepository<AuditFileEntity, Long> {
}
