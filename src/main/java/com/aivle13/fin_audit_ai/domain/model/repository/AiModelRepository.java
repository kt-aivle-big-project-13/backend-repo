package com.aivle13.fin_audit_ai.domain.model.repository;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiModelRepository extends JpaRepository<AiModelEntity, Long> {
}
