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
import com.aivle13.fin_audit_ai.global.exception.model.AuditAlreadyInProgressException;
import com.aivle13.fin_audit_ai.global.exception.model.DatasetNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.InvalidSensitiveAttributeException;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.SensitiveAttributesNotSelectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditStartService {

    private static final List<AuditStatus> ACTIVE_STATUSES = List.of(AuditStatus.PENDING, AuditStatus.IN_PROGRESS);

    private final AiModelRepository aiModelRepository;
    private final DatasetRepository datasetRepository;
    private final AuditRepository auditRepository;
    private final AuditService auditService;

    @Transactional
    public AuditStartResponse start(Long userId, AuditStartRequest request) {
        // 락으로 같은 모델에 대한 동시 감사 시작 요청을 직렬화해, 중복 체크와 생성 사이의
        // race condition으로 진행 중 감사가 2건 이상 생기는 것을 막는다.
        AiModelEntity model = aiModelRepository.findByIdForUpdate(request.modelId())
                .orElseThrow(ModelNotFoundException::new);

        DatasetEntity dataset = datasetRepository.findById(request.datasetId())
                .orElseThrow(DatasetNotFoundException::new);
        if (!dataset.getModel().getId().equals(model.getId())) {
            throw new DatasetNotFoundException();
        }

        if (model.getSensitiveAttributes() == null) {
            throw new SensitiveAttributesNotSelectedException();
        }

        // 민감정보는 모델 단위로 저장되지만, 이후 다른 데이터셋이 재업로드되면
        // 컬럼명이 바뀌거나 사라질 수 있어 실제로 선택된 데이터셋 기준으로 다시 검증한다.
        Set<String> datasetColumns = Arrays.stream(dataset.getColumns().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        boolean allSensitiveAttributesExist = Arrays.stream(model.getSensitiveAttributes().split(","))
                .map(String::trim)
                .allMatch(datasetColumns::contains);
        if (!allSensitiveAttributesExist) {
            throw new InvalidSensitiveAttributeException();
        }

        if (auditRepository.existsByModel_IdAndStatusIn(model.getId(), ACTIVE_STATUSES)) {
            throw new AuditAlreadyInProgressException();
        }

        AuditEntity audit = auditService.create(
                userId, model, dataset, request.auditName(), model.getSensitiveAttributes(), request.assessmentId()
        );

        return new AuditStartResponse(audit.getId(), audit.getStatus().name(), audit.getCreatedAt());
    }
}
