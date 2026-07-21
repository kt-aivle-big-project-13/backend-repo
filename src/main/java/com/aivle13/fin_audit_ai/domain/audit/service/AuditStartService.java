package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.AuditStartRequestDto;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.AuditStartResponseDto;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.global.exception.model.AuditAlreadyInProgressException;
import com.aivle13.fin_audit_ai.global.exception.model.DatasetNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.SensitiveAttributesNotSelectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditStartService {

    private static final List<AuditStatus> ACTIVE_STATUSES = List.of(AuditStatus.PENDING, AuditStatus.IN_PROGRESS);

    private final AiModelRepository aiModelRepository;
    private final DatasetRepository datasetRepository;
    private final AuditRepository auditRepository;
    private final AuditService auditService;

    @Transactional
    public AuditStartResponseDto start(Long userId, AuditStartRequestDto request) {
        AiModelEntity model = aiModelRepository.findById(request.modelId())
                .orElseThrow(ModelNotFoundException::new);

        DatasetEntity dataset = datasetRepository.findById(request.datasetId())
                .orElseThrow(DatasetNotFoundException::new);
        if (!dataset.getModel().getId().equals(model.getId())) {
            throw new DatasetNotFoundException();
        }

        if (model.getSensitiveAttributes() == null) {
            throw new SensitiveAttributesNotSelectedException();
        }

        if (auditRepository.existsByModel_IdAndStatusIn(model.getId(), ACTIVE_STATUSES)) {
            throw new AuditAlreadyInProgressException();
        }

        AuditEntity audit = auditService.create(
                userId, model, dataset, request.auditName(), model.getSensitiveAttributes(), request.assessmentId()
        );

        return new AuditStartResponseDto(audit.getId(), audit.getStatus().name(), audit.getCreatedAt());
    }
}
