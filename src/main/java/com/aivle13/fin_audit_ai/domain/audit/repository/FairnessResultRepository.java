package com.aivle13.fin_audit_ai.domain.audit.repository;

import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FairnessResultRepository
        extends JpaRepository<FairnessResultEntity, Long> {

    List<FairnessResultEntity> findAllByAudit_Id(Long auditId);

    void deleteAllByAudit_Id(Long auditId);
}
