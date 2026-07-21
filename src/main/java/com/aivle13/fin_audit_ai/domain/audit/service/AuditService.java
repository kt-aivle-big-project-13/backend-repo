package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;
    private final UserRepository userRepository;

    public AuditEntity create(Long userId, AiModelEntity aiModel, String datasetPath, String sensitiveFeatures) {
        UserEntity user = userRepository.getReferenceById(userId);
        AuditEntity audit = AuditEntity.create(aiModel, user, datasetPath, sensitiveFeatures);

        return auditRepository.save(audit);
    }
}