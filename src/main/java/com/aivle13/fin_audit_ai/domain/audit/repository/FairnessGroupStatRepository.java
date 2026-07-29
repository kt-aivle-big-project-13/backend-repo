package com.aivle13.fin_audit_ai.domain.audit.repository;

import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessGroupStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FairnessGroupStatRepository
        extends JpaRepository<FairnessGroupStatEntity, Long> {

    List<FairnessGroupStatEntity> findAllByAudit_Id(Long auditId);

    List<FairnessGroupStatEntity> findAllByAudit_IdAndAttribute(Long auditId, String attribute);

    void deleteAllByAudit_Id(Long auditId);
}
