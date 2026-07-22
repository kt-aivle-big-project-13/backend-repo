package com.aivle13.fin_audit_ai.domain.audit.repository;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuditRepository extends JpaRepository<AuditEntity, Long> {

    Optional<AuditEntity> findByIdAndUser_Id(Long auditId, Long userId);

    boolean existsByModel_IdAndStatusIn(Long modelId, List<AuditStatus> statuses);

    @Query("""
            select audit
            from AuditEntity audit
            join fetch audit.model
            join fetch audit.dataset
            where audit.id = :auditId
            """)
    Optional<AuditEntity> findByIdWithModelAndDataset(
            @Param("auditId") Long auditId
    );
}
