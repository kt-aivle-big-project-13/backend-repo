package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditProgressService {

    private static final int FAIRNESS_STEP = 3;
    private static final int COMPLETED_STEP = 4;

    private final AuditRepository auditRepository;

    @Transactional
    public void markInProgress(Long auditId) {
        AuditEntity audit = findAudit(auditId);
        audit.markInProgress();
    }

    @Transactional
    public void markShapCompleted(Long auditId) {
        AuditEntity audit = findAudit(auditId);
        audit.moveToStep(FAIRNESS_STEP);
    }

    @Transactional
    public void markFairnessCompleted(Long auditId) {
        AuditEntity audit = findAudit(auditId);
        audit.moveToStep(COMPLETED_STEP);
    }

    @Transactional
    public void markFailed(Long auditId) {
        AuditEntity audit = findAudit(auditId);
        audit.markFailed();
    }

    private AuditEntity findAudit(Long auditId) {
        return auditRepository.findById(auditId)
                .orElseThrow(AuditNotFoundException::new);
    }
}