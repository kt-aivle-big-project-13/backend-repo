package com.aivle13.fin_audit_ai.domain.model.service;

import com.aivle13.fin_audit_ai.domain.model.dto.response.DatasetSummaryResponse;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import com.aivle13.fin_audit_ai.global.config.CacheConfig;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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
    // 캐시 무효화(DatasetUploadService)는 업로드에 쓰인 modelId 기준이라, 같은 modelGroupId의
    // 다른 modelId로 먼저 캐시를 채운 뒤 그 modelId로 업로드가 없으면 새 데이터셋이 반영되지
    // 않을 수 있다 (TTL로 최종 정합성 보장).
    @Cacheable(cacheNames = CacheConfig.DATASETS_CACHE, key = "#userId + ':' + #modelId + ':' + #purpose")
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
