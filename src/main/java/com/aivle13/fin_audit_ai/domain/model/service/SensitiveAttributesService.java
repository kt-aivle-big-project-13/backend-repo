package com.aivle13.fin_audit_ai.domain.model.service;

import com.aivle13.fin_audit_ai.domain.model.dto.request.SensitiveAttributesRequest;
import com.aivle13.fin_audit_ai.domain.model.dto.response.SensitiveAttributesResponse;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.global.exception.model.DatasetNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.InvalidSensitiveAttributeException;
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

    private final DatasetRepository datasetRepository;

    @Transactional
    public SensitiveAttributesResponse update(Long userId, Long modelId, Long datasetId, SensitiveAttributesRequest request) {
        DatasetEntity dataset = datasetRepository.findByIdAndModel_IdAndModel_User_Id(datasetId, modelId, userId)
                .orElseThrow(DatasetNotFoundException::new);

        Set<String> datasetColumns = Arrays.stream(dataset.getColumns().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        List<String> sensitiveAttributes = request.sensitiveAttributes();
        boolean allColumnsExist = sensitiveAttributes.stream().allMatch(datasetColumns::contains);
        if (!allColumnsExist) {
            throw new InvalidSensitiveAttributeException();
        }

        dataset.updateSensitiveAttributes(String.join(",", sensitiveAttributes));

        return new SensitiveAttributesResponse(dataset.getId(), sensitiveAttributes);
    }
}
