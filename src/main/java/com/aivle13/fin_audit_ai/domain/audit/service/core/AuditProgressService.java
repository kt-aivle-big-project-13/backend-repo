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
import com.aivle13.fin_audit_ai.domain.notification.service.NotificationService;
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
    private final NotificationService notificationService;

    @Transactional
    public void markInProgress(Long auditId, int generation) {
        AuditEntity audit = findAuditForUpdate(auditId);
        if (isStaleOrCancelled(audit, generation)) {
            return;
        }
        audit.markInProgress();
    }

    @Transactional
    public void markShapCompleted(Long auditId, int generation) {
        AuditEntity audit = findAuditForUpdate(auditId);
        if (isStaleOrCancelled(audit, generation)) {
            return;
        }
        audit.moveToStep(FAIRNESS_STEP);
    }

    @Transactional
    public void markFairnessCompleted(Long auditId, int generation) {
        AuditEntity audit = findAuditForUpdate(auditId);
        if (isStaleOrCancelled(audit, generation)) {
            return;
        }
        AuditStatus verdict = determineVerdict(auditId);
        audit.complete(COMPLETED_STEP, verdict);
        notificationService.notifyAuditComplete(audit);

        // 판정이 '주의(WARNING)' 이상(WARNING, NON_COMPLIANT)인 경우에만 재감사 권고 알림을 보낸다.
        if (verdict != AuditStatus.COMPLIANT) {
            notificationService.notifyReauditRecommend(audit);
        }
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
    public void markFailed(Long auditId, int generation) {
        AuditEntity audit = findAuditForUpdate(auditId);
        if (isStaleOrCancelled(audit, generation)) {
            return;
        }
        // 같은 auditId·generation으로 markFailed가 두 번 불려도(이벤트 중복 발행 등) 이미
        // 이 실행에서 FAILED 처리가 끝났다면 다시 처리하지 않는다 - 안 그러면 알림이
        // 중복 생성된다. 재시도(retry())는 상태를 PENDING으로 되돌리고 generation을
        // 올리므로, 새로 실패한 실행은 이 가드에 걸리지 않고 정상적으로 알림을 받는다.
        if (audit.getStatus() == AuditStatus.FAILED) {
            return;
        }
        audit.markFailed();
        notificationService.notifyAuditFailed(audit);
    }

    // SHAP·Fairlearn 재시도 콜백과 무관하게(자율점검 법령 매핑 실패, 서버 재시작 복구 등)
    // 특정 실행 세대를 알 수 없는 호출부용. 세대는 안 보고 취소 여부만 확인한다.
    @Transactional
    public void markFailed(Long auditId) {
        AuditEntity audit = findAuditForUpdate(auditId);
        if (audit.isCancelled()) {
            return;
        }
        audit.markFailed();
    }

    // 취소됐거나(같은 실행이 중간에 취소됨), 이 콜백이 이미 재시도로 지나가버린 실행
    // 세대의 것이면(늦게 도착한 이전 실행의 응답) 이후 단계를 더 진행할 필요가 없으므로,
    // 이벤트 리스너가 다음 단계를 건너뛸지 판단하는 데 쓴다. 어차피 각 markXxx 단계의
    // 잠금 있는 재확인이 최종 방어선이라, 여긴 잠금 없이 가볍게 조회한다.
    @Transactional(readOnly = true)
    public boolean isCancelled(Long auditId, int generation) {
        AuditEntity audit = findAudit(auditId);
        return audit.isCancelled() || audit.getGeneration() != generation;
    }

    // 취소됐거나 이미 재시도로 지나가버린 실행 세대의 콜백이면 상태 변경을 무시한다.
    private boolean isStaleOrCancelled(AuditEntity audit, int generation) {
        return audit.isCancelled() || audit.getGeneration() != generation;
    }

    private AuditEntity findAudit(Long auditId) {
        return auditRepository.findById(auditId)
                .orElseThrow(AuditNotFoundException::new);
    }

    // 사용자가 취소/재시도 API로 같은 감사를 동시에 건드릴 수 있으므로, 상태를 실제로
    // 바꾸는 전이는 쓰기 잠금으로 조회해 그 사이의 경합(늦게 도착한 콜백이 취소 상태를
    // 덮어쓰는 것 등)을 막는다.
    private AuditEntity findAuditForUpdate(Long auditId) {
        return auditRepository.findByIdForUpdate(auditId)
                .orElseThrow(AuditNotFoundException::new);
    }
}