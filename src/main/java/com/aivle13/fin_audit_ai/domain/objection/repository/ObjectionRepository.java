package com.aivle13.fin_audit_ai.domain.objection.repository;

import com.aivle13.fin_audit_ai.domain.objection.entity.ObjectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ObjectionRepository
        extends JpaRepository<ObjectionEntity, Long>, JpaSpecificationExecutor<ObjectionEntity> {

    boolean existsByObjectionNo(String objectionNo);
}