package com.aivle13.fin_audit_ai.domain.model.service;

import com.aivle13.fin_audit_ai.domain.model.dto.request.SensitiveAttributesRequest;
import com.aivle13.fin_audit_ai.domain.model.dto.response.SensitiveAttributesResponse;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.global.exception.model.InvalidSensitiveAttributeException;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SensitiveAttributesService {

    private final AiModelRepository aiModelRepository;
    private final DatasetRepository datasetRepository;

    @Transactional
    public SensitiveAttributesResponse update(Long modelId, SensitiveAttributesRequest request) {
        AiModelEntity model = aiModelRepository.findById(modelId)
                .orElseThrow(ModelNotFoundException::new);

        // 데이터셋이 없으면 검증 기준 컬럼 자체가 없어 어떤 값도 유효할 수 없음
        DatasetEntity dataset = datasetRepository.findTopByModel_IdOrderByCreatedAtDesc(modelId)
                .orElseThrow(InvalidSensitiveAttributeException::new);

        Set<String> datasetColumns = Arrays.stream(dataset.getColumns().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        List<String> sensitiveAttributes = request.sensitiveAttributes();
        boolean allColumnsExist = sensitiveAttributes.stream().allMatch(datasetColumns::contains);
        if (!allColumnsExist) {
            throw new InvalidSensitiveAttributeException();
        }

        model.updateSensitiveAttributes(String.join(",", sensitiveAttributes));

        return new SensitiveAttributesResponse(model.getId(), sensitiveAttributes);
    }
}
