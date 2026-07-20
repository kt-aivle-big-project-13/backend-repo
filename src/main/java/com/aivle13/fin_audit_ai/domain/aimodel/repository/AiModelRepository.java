package com.aivle13.fin_audit_ai.domain.aimodel.repository;

import com.aivle13.fin_audit_ai.domain.aimodel.entity.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiModelRepository extends JpaRepository<AiModel, Long> {
}
