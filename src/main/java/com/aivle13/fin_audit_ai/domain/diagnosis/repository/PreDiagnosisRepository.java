package com.aivle13.fin_audit_ai.domain.diagnosis.repository;

import com.aivle13.fin_audit_ai.domain.diagnosis.entity.PreDiagnosisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreDiagnosisRepository
        extends JpaRepository<PreDiagnosisEntity, Long> {

    Optional<PreDiagnosisEntity> findByIdAndUser_Id(
            Long assessmentId,
            Long userId
    );
}
