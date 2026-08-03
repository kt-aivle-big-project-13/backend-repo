package com.aivle13.fin_audit_ai.domain.objection.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.ShapFeatureImportanceRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionModelEvidenceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObjectionModelEvidenceService {

    private static final List<AuditStatus> COMPLETED_STATUSES = List.of(
            AuditStatus.COMPLIANT,
            AuditStatus.WARNING,
            AuditStatus.NON_COMPLIANT,
            AuditStatus.UNCONFIRMED
    );

    private final AuditRepository auditRepository;
    private final ShapFeatureImportanceRepository
            shapFeatureImportanceRepository;

    public List<ObjectionModelEvidenceResponse> findLatestTopEvidence(
            AiModelEntity model
    ) {
        if (model == null) {
            return List.of();
        }

        return auditRepository
                .findFirstByModel_IdAndStatusInOrderByCompletedAtDescIdDesc(
                        model.getId(),
                        COMPLETED_STATUSES
                )
                .map(this::findTopEvidence)
                .orElseGet(List::of);
    }

    private List<ObjectionModelEvidenceResponse> findTopEvidence(
            AuditEntity audit
    ) {
        return shapFeatureImportanceRepository
                .findTop5ByAudit_IdAndSensitiveFalseOrderByRankAsc(
                        audit.getId()
                )
                .stream()
                .map(ObjectionModelEvidenceResponse::from)
                .toList();
    }
}