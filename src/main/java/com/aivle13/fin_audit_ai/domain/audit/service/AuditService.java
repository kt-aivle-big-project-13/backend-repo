package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.aimodel.entity.AiModel;
import com.aivle13.fin_audit_ai.domain.audit.entity.Audit;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;

    public Audit create(Long userId, AiModel aiModel, String datasetPath) {
        Audit audit = Audit.builder()
                .userId(userId)
                .aiModel(aiModel)
                .datasetPath(datasetPath)
                .status(AuditStatus.UPLOADED)
                .build();

        return auditRepository.save(audit);
    }
}