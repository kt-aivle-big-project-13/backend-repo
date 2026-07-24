package com.aivle13.fin_audit_ai.domain.model.service.model;

import com.aivle13.fin_audit_ai.domain.model.dto.response.model.ModelSummaryResponse;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelQueryService {

    private final AiModelRepository aiModelRepository;

    // modelGroupId(모델 계열)당 최신 버전 1건만 반환한다.
    // createdAt desc, id desc(동률 결정적 정렬용 보조 기준)로 정렬된 결과를 LinkedHashMap으로
    // dedupe하면 각 그룹의 첫 등장 = 최신 버전이 남고, 삽입 순서도 그대로 유지되어 재정렬이 필요 없다.
    public List<ModelSummaryResponse> list(Long userId) {
        List<AiModelEntity> models = aiModelRepository.findByUser_IdOrderByCreatedAtDescIdDesc(userId);

        return models.stream()
                .collect(Collectors.toMap(
                        AiModelEntity::getModelGroupId, m -> m, (first, second) -> first, LinkedHashMap::new))
                .values().stream()
                .map(ModelSummaryResponse::from)
                .toList();
    }
}
