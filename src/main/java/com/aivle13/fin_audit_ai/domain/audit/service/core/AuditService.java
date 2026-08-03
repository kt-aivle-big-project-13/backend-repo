package com.aivle13.fin_audit_ai.domain.audit.service.core;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.core.AuditSummaryResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.ThresholdMethod;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotCancellableException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;
    private final UserRepository userRepository;

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
}
