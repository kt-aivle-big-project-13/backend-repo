package com.aivle13.fin_audit_ai.domain.model.repository;

import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DatasetRepository extends JpaRepository<DatasetEntity, Long> {
    Optional<DatasetEntity> findByIdAndModel_IdAndModel_User_Id(Long id, Long modelId, Long userId);

    boolean existsByModel_IdAndPurpose(Long modelId, DatasetPurpose purpose);
}
