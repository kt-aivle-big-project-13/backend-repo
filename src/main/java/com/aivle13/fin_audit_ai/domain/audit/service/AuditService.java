package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.ThresholdMethod;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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
}
