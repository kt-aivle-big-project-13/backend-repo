package com.aivle13.fin_audit_ai.domain.audit.repository;

import com.aivle13.fin_audit_ai.domain.audit.entity.ShapFeatureImportanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShapFeatureImportanceRepository
        extends JpaRepository<ShapFeatureImportanceEntity, Long> {

    List<ShapFeatureImportanceEntity> findAllByAudit_IdOrderByRankAsc(Long auditId);

    List<ShapFeatureImportanceEntity> findTop5ByAudit_IdAndSensitiveFalseOrderByRankAsc(Long auditId);

    void deleteAllByAudit_Id(Long auditId);
}
