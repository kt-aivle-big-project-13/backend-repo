package com.aivle13.fin_audit_ai.domain.audit.repository;

import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiMetricCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface XaiResultRepository
        extends JpaRepository<XaiResultEntity, Long> {

    List<XaiResultEntity> findAllByAudit_Id(Long auditId);

    List<XaiResultEntity> findAllByAudit_IdAndMetricCodeIn(
            Long auditId,
            Collection<XaiMetricCode> metricCodes
    );

    void deleteAllByAudit_IdAndMetricCodeIn(
            Long auditId,
            Collection<XaiMetricCode> metricCodes
    );
}