package com.aivle13.fin_audit_ai.domain.model.repository;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiModelRepository extends JpaRepository<AiModelEntity, Long> {
    Optional<AiModelEntity> findTopByUser_IdAndModelNameOrderByVersionDesc(Long userId, String modelName);
}
