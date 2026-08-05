package com.aivle13.fin_audit_ai.domain.model.service.dataset;

import com.aivle13.fin_audit_ai.domain.model.dto.response.dataset.DatasetSummaryResponse;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import com.aivle13.fin_audit_ai.global.config.CacheConfig;
import com.aivle13.fin_audit_ai.global.exception.model.core.ModelNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

        // 이 반환값 자체가 Redis에 캐시된다. Stream.toList()가 돌려주는 JDK 내부 불변 리스트는
        // 최상위 캐시 값으로 직렬화될 때 타입 정보가 깨져 캐시 조회(역직렬화) 시 500이 나므로,
        // 반드시 평범한 ArrayList로 감싸 캐시-안전하게 유지한다.
        return new ArrayList<>(datasets.stream().map(DatasetSummaryResponse::from).toList());
    }
}
