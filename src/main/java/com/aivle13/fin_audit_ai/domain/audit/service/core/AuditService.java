package com.aivle13.fin_audit_ai.domain.audit.service.core;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.core.AuditRetryResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.core.AuditSummaryResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.event.AuditStartedEvent;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.ThresholdMethod;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.global.exception.model.AuditAlreadyInProgressException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotCancellableException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotRetryableException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final List<AuditStatus> ACTIVE_STATUSES = List.of(AuditStatus.PENDING, AuditStatus.IN_PROGRESS);

    private final AuditRepository auditRepository;
    private final UserRepository userRepository;
    private final XaiResultRepository xaiResultRepository;
    private final FairnessResultRepository fairnessResultRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AuditEntity create(Long userId, AiModelEntity model, DatasetEntity dataset, String auditName,
                               String sensitiveFeatures, Long assessmentId, ThresholdMethod thresholdMethod,
                               BigDecimal targetApprovalRate, BigDecimal manualThreshold,
                               DatasetEntity validationDataset) {
        UserEntity user = userRepository.getReferenceById(userId);
        AuditEntity audit = AuditEntity.create(model, dataset, user, auditName, sensitiveFeatures, assessmentId,
                thresholdMethod, targetApprovalRate, manualThreshold, validationDataset);

        return auditRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public List<AuditSummaryResponse> list(Long userId) {
        return auditRepository.findByUser_IdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(AuditSummaryResponse::from)
                .toList();
    }

    // AI 서버로 이미 나간 분석 요청 자체를 끊지는 않는 soft cancel이라, 동시에 도착하는
    // 늦은 콜백과의 경합을 막기 위해 쓰기 잠금으로 조회한다.
    @Transactional
    public void cancel(Long auditId, Long userId) {
        AuditEntity audit = auditRepository.findByIdAndUser_IdForUpdate(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);

        if (!audit.isCancellable()) {
            throw new AuditNotCancellableException();
        }

        audit.cancel();
    }

    // FAILED·CANCELLED 상태만 재시도할 수 있고, 이전 실행에서 남은 산출물이 새 분석 결과와
    // 섞이지 않도록 기존 SHAP·공정성 결과를 먼저 지운 뒤 감사를 초기화하고 분석을 다시 발행한다.
    @Transactional
    public AuditRetryResponse retry(Long auditId, Long userId) {
        AuditEntity audit = auditRepository.findByIdAndUser_IdForUpdate(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);

        if (!audit.isRetryable()) {
            throw new AuditNotRetryableException();
        }

        // 이 감사가 실패·취소된 뒤 같은 모델로 새 감사를 따로 시작했을 수 있으므로,
        // 재시도로 두 감사가 동시에 활성화되지 않도록 AuditStartService.start()와
        // 동일한 규칙으로 막는다.
        if (auditRepository.existsByModel_IdAndStatusIn(audit.getModel().getId(), ACTIVE_STATUSES)) {
            throw new AuditAlreadyInProgressException();
        }

        xaiResultRepository.deleteAllByAudit_Id(auditId);
        fairnessResultRepository.deleteAllByAudit_Id(auditId);
        audit.retry();

        eventPublisher.publishEvent(new AuditStartedEvent(auditId));

        return AuditRetryResponse.from(audit, LocalDateTime.now());
    }
}
