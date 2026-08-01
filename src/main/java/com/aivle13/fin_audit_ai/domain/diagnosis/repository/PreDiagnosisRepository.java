package com.aivle13.fin_audit_ai.domain.diagnosis.repository;

import com.aivle13.fin_audit_ai.domain.diagnosis.entity.PreDiagnosisEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreDiagnosisRepository
        extends JpaRepository<PreDiagnosisEntity, Long> {

    Optional<PreDiagnosisEntity> findByIdAndUser_Id(
            Long assessmentId,
            Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select diagnosis
        from PreDiagnosisEntity diagnosis
        where diagnosis.id = :assessmentId
          and diagnosis.user.id = :userId
        """)
    Optional<PreDiagnosisEntity> findByIdAndUser_IdForUpdate(
            @Param("assessmentId") Long assessmentId,
            @Param("userId") Long userId
    );
}
