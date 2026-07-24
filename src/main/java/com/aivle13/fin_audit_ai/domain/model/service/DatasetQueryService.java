package com.aivle13.fin_audit_ai.domain.model.service;

import com.aivle13.fin_audit_ai.domain.model.dto.response.DatasetSummaryResponse;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import com.aivle13.fin_audit_ai.global.config.CacheConfig;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DatasetQueryService {

    private final AiModelRepository aiModelRepository;
    private final DatasetRepository datasetRepository;

    // list()에서 @Cacheable이 붙은 listByModelGroup()을 this로 직접 호출하면
    // 프록시를 우회해 캐싱이 적용되지 않는다. 자기 자신의 프록시를 주입받아 그걸 통해 호출한다.
    private final DatasetQueryService self;

    public DatasetQueryService(
            AiModelRepository aiModelRepository,
            DatasetRepository datasetRepository,
            @Lazy DatasetQueryService self
    ) {
        this.aiModelRepository = aiModelRepository;
        this.datasetRepository = datasetRepository;
        this.self = self;
    }

    // modelId로 소유권을 확인한 뒤, 캐시는 실제 조회 단위인 modelGroupId 기준으로 적용한다.
    // 이렇게 하면 DatasetUploadService.evictCache가 같은 modelGroupId 키를 무효화했을 때
    // 이 메서드가 채운 캐시도 함께 무효화되어 형제 modelId 간 정합성이 깨지지 않는다.
    public List<DatasetSummaryResponse> list(Long userId, Long modelId, DatasetPurpose purpose) {
        AiModelEntity model = aiModelRepository.findByIdAndUser_Id(modelId, userId)
                .orElseThrow(ModelNotFoundException::new);

        return self.listByModelGroup(userId, model.getModelGroupId(), purpose);
    }

    @Cacheable(cacheNames = CacheConfig.DATASETS_CACHE, key = "#userId + ':' + #modelGroupId + ':' + #purpose")
    public List<DatasetSummaryResponse> listByModelGroup(Long userId, String modelGroupId, DatasetPurpose purpose) {
        List<DatasetEntity> datasets = purpose != null
                ? datasetRepository.findByModel_User_IdAndModel_ModelGroupIdAndPurposeOrderByCreatedAtDesc(
                        userId, modelGroupId, purpose)
                : datasetRepository.findByModel_User_IdAndModel_ModelGroupIdOrderByCreatedAtDesc(
                        userId, modelGroupId);

        return datasets.stream().map(DatasetSummaryResponse::from).toList();
    }
}
