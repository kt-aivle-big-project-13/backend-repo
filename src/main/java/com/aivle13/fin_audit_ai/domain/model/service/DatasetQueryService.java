package com.aivle13.fin_audit_ai.domain.model.service;

import com.aivle13.fin_audit_ai.domain.model.dto.response.DatasetSummaryResponse;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DatasetQueryService {

    private final AiModelRepository aiModelRepository;
    private final DatasetRepository datasetRepository;

    // modelId는 조회 대상 모델 계열을 특정하고 소유권을 확인하는 용도로만 쓰이고,
    // 실제 목록은 같은 modelGroupId를 공유하는 다른 버전의 데이터셋까지 전부 포함한다.
    public List<DatasetSummaryResponse> list(Long userId, Long modelId, DatasetPurpose purpose) {
        AiModelEntity model = aiModelRepository.findByIdAndUser_Id(modelId, userId)
                .orElseThrow(ModelNotFoundException::new);

        List<DatasetEntity> datasets = purpose != null
                ? datasetRepository.findByModel_User_IdAndModel_ModelGroupIdAndPurposeOrderByCreatedAtDesc(
                        userId, model.getModelGroupId(), purpose)
                : datasetRepository.findByModel_User_IdAndModel_ModelGroupIdOrderByCreatedAtDesc(
                        userId, model.getModelGroupId());

        return datasets.stream().map(DatasetSummaryResponse::from).toList();
    }
}
