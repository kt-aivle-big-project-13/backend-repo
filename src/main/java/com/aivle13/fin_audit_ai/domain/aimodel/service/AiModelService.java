package com.aivle13.fin_audit_ai.domain.aimodel.service;

import com.aivle13.fin_audit_ai.domain.aimodel.entity.AiModel;
import com.aivle13.fin_audit_ai.domain.aimodel.repository.AiModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiModelService {

    private final AiModelRepository aiModelRepository;

    public AiModel create(Long userId, String modelName, ModelType modelType, String artifactPath) {
        AiModel aiModel = AiModel.builder()
                .userId(userId)
                .modelName(modelName)
                .modelType(modelType)
                .version(1)
                .artifactPath(artifactPath)
                .status(AiModelStatus.ACTIVE)
                .build();

        return aiModelRepository.save(aiModel);
    }

}
