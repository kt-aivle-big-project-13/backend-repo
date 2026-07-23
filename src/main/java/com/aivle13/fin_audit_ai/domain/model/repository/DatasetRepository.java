package com.aivle13.fin_audit_ai.domain.model.repository;

import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatasetRepository extends JpaRepository<DatasetEntity, Long> {
    Optional<DatasetEntity> findByIdAndModel_IdAndModel_User_Id(Long id, Long modelId, Long userId);

    Optional<DatasetEntity> findFirstByModel_ModelGroupIdAndPurposeOrderByCreatedAtDesc(String modelGroupId, DatasetPurpose purpose);

    List<DatasetEntity> findByModel_User_IdAndModel_ModelGroupIdOrderByCreatedAtDesc(Long userId, String modelGroupId);

    List<DatasetEntity> findByModel_User_IdAndModel_ModelGroupIdAndPurposeOrderByCreatedAtDesc(
            Long userId, String modelGroupId, DatasetPurpose purpose);
}
