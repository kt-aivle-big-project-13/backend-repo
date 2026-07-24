package com.aivle13.fin_audit_ai.domain.audit.service.core;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiStatus;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditProgressService {

    private static final int FAIRNESS_STEP = 3;
    private static final int COMPLETED_STEP = 4;

    private final AuditRepository auditRepository;
    private final FairnessResultRepository fairnessResultRepository;
    private final XaiResultRepository xaiResultRepository;

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
        audit.complete(COMPLETED_STEP, determineVerdict(auditId));
    }

    // 저장된 공정성·설명가능성 지표 판정을 종합해 감사 준수 상태를 산출한다.
    //  - 공정성 지표에 FAIL 이 하나라도 있으면 NON_COMPLIANT
    //  - FAIL 은 없고 REVIEW(공정성 또는 XAI) 가 있으면 WARNING
    //  - 모두 PASS 면 COMPLIANT
    private AuditStatus determineVerdict(Long auditId) {
        List<FairnessResultEntity> fairnessResults =
                fairnessResultRepository.findAllByAudit_Id(auditId);
        List<XaiResultEntity> xaiResults =
                xaiResultRepository.findAllByAudit_Id(auditId);

        boolean hasFail = fairnessResults.stream()
                .anyMatch(result -> result.getStatus() == FairnessStatus.FAIL);

        if (hasFail) {
            return AuditStatus.NON_COMPLIANT;
        }

        boolean hasReview = fairnessResults.stream()
                .anyMatch(result -> result.getStatus() == FairnessStatus.REVIEW)
                || xaiResults.stream()
                .anyMatch(result -> result.getStatus() == XaiStatus.REVIEW);

        return hasReview ? AuditStatus.WARNING : AuditStatus.COMPLIANT;
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