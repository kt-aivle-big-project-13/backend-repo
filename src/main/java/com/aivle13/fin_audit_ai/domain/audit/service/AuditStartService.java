package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.AuditStartRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.AuditStartResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.ThresholdMethod;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import com.aivle13.fin_audit_ai.global.exception.model.AuditAlreadyInProgressException;
import com.aivle13.fin_audit_ai.global.exception.model.DatasetNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.IncompatibleDatasetSchemaException;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.SensitiveAttributesNotSelectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aivle13.fin_audit_ai.domain.audit.event.AuditStartedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

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
        // 데이터셋이 반드시 이 모델 row에 속할 필요는 없다. 같은 사용자의 같은 모델 계열
        // (modelGroupId)이기만 하면 이전 버전에 올린 데이터셋도 재사용할 수 있다.
        if (!dataset.getModel().getUser().getId().equals(userId)
                || !dataset.getModel().getModelGroupId().equals(model.getModelGroupId())) {
            throw new DatasetNotFoundException();
        }

        if (dataset.getPurpose() != DatasetPurpose.AUDIT) {
            throw new DatasetNotFoundException();
        }

        // 모델 스펙 자체를 저장하는 곳이 없어서, 계열 내 가장 최근 감사 데이터셋의 컬럼 구성을
        // 기준으로 삼아 재사용하려는 데이터셋과 비교한다.
        Optional<DatasetEntity> latestInGroup = datasetRepository
                .findFirstByModel_ModelGroupIdAndPurposeOrderByCreatedAtDesc(model.getModelGroupId(), DatasetPurpose.AUDIT);
        if (latestInGroup.isPresent() && !latestInGroup.get().getColumns().equals(dataset.getColumns())) {
            throw new IncompatibleDatasetSchemaException();
        }

        // 민감정보는 데이터셋 버전 단위로 저장되므로, 이 데이터셋에 저장된 값이
        // 곧 이 데이터셋의 실제 컬럼 기준으로 항상 유효함이 보장된다.
        if (dataset.getSensitiveAttributes() == null) {
            throw new SensitiveAttributesNotSelectedException();
        }

        if (auditRepository.existsByModel_IdAndStatusIn(model.getId(), ACTIVE_STATUSES)) {
            throw new AuditAlreadyInProgressException();
        }

        DatasetEntity validationDataset = resolveValidationDataset(userId, model, request);

        AuditEntity audit = auditService.create(
                userId, model, dataset, request.auditName(), dataset.getSensitiveAttributes(), request.assessmentId(),
                request.thresholdMethod(), request.targetApprovalRate(), request.manualThreshold(), validationDataset
        );
        dataset.markAudited();

        eventPublisher.publishEvent(
                new AuditStartedEvent(audit.getId())
        );

        return new AuditStartResponse(audit.getId(), audit.getStatus().name(), audit.getCreatedAt());
    }

    // MANUAL이면 검증 데이터셋이 필요 없다. VALIDATION_DATASET이면 사용자가 지정한 데이터셋을
    // 검증해서 쓰거나, 지정하지 않았으면 같은 모델 계열의 최신 VALIDATION 데이터셋을 자동 선택한다.
    // 계열에 검증 데이터셋이 하나도 없으면 null을 반환하고, AI 서버가 감사 데이터셋으로 폴백한다.
    private DatasetEntity resolveValidationDataset(Long userId, AiModelEntity model, AuditStartRequest request) {
        if (request.thresholdMethod() != ThresholdMethod.VALIDATION_DATASET) {
            return null;
        }

        if (request.validationDatasetId() != null) {
            DatasetEntity validationDataset = datasetRepository.findById(request.validationDatasetId())
                    .orElseThrow(DatasetNotFoundException::new);
            if (!validationDataset.getModel().getUser().getId().equals(userId)
                    || !validationDataset.getModel().getModelGroupId().equals(model.getModelGroupId())
                    || validationDataset.getPurpose() != DatasetPurpose.VALIDATION) {
                throw new DatasetNotFoundException();
            }
            return validationDataset;
        }

        return datasetRepository
                .findFirstByModel_ModelGroupIdAndPurposeOrderByCreatedAtDesc(model.getModelGroupId(), DatasetPurpose.VALIDATION)
                .orElse(null);
    }
}
