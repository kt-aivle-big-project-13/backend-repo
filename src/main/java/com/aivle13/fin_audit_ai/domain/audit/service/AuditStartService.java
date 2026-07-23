package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.AuditStartRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.AuditStartResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import com.aivle13.fin_audit_ai.global.exception.model.AuditAlreadyInProgressException;
import com.aivle13.fin_audit_ai.global.exception.model.DatasetNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.SensitiveAttributesNotSelectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aivle13.fin_audit_ai.domain.audit.event.AuditStartedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditStartService {

    private static final List<AuditStatus> ACTIVE_STATUSES = List.of(AuditStatus.PENDING, AuditStatus.IN_PROGRESS);

    private final AiModelRepository aiModelRepository;
    private final DatasetRepository datasetRepository;
    private final AuditRepository auditRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuditStartResponse start(Long userId, AuditStartRequest request) {
        // 소유자 스코프 + 락으로, 다른 사용자의 모델 조회 자체를 막으면서 동시에 같은 모델에 대한
        // 동시 감사 시작 요청을 직렬화해 중복 체크와 생성 사이의 race condition도 막는다.
        AiModelEntity model = aiModelRepository.findByIdAndUser_IdForUpdate(request.modelId(), userId)
                .orElseThrow(ModelNotFoundException::new);

        DatasetEntity dataset = datasetRepository.findById(request.datasetId())
                .orElseThrow(DatasetNotFoundException::new);
        if (!dataset.getModel().getId().equals(model.getId())) {
            throw new DatasetNotFoundException();
        }
        if (dataset.getPurpose() != DatasetPurpose.AUDIT) {
            throw new DatasetNotFoundException();
        }

        // 민감정보는 데이터셋 버전 단위로 저장되므로, 이 데이터셋에 저장된 값이
        // 곧 이 데이터셋의 실제 컬럼 기준으로 항상 유효함이 보장된다.
        if (dataset.getSensitiveAttributes() == null) {
            throw new SensitiveAttributesNotSelectedException();
        }

        if (auditRepository.existsByModel_IdAndStatusIn(model.getId(), ACTIVE_STATUSES)) {
            throw new AuditAlreadyInProgressException();
        }

        AuditEntity audit = auditService.create(
                userId, model, dataset, request.auditName(), dataset.getSensitiveAttributes(), request.assessmentId(),
                request.thresholdMethod(), request.targetApprovalRate(), request.manualThreshold()
        );
        dataset.markAudited();

        eventPublisher.publishEvent(
                new AuditStartedEvent(audit.getId())
        );

        return new AuditStartResponse(audit.getId(), audit.getStatus().name(), audit.getCreatedAt());
    }
}
