package com.aivle13.fin_audit_ai.domain.audit.repository;

import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SelfCheckAnswerRepository
        extends JpaRepository<SelfCheckAnswerEntity, Long> {

    List<SelfCheckAnswerEntity> findAllByAudit_Id(Long auditId);

    void deleteAllByAudit_Id(Long auditId);
}