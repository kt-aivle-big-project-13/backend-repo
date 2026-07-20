package com.aivle13.fin_audit_ai.domain.model.service;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiModelService {

    private final AiModelRepository aiModelRepository;
    private final UserRepository userRepository;

    public AiModelEntity create(Long userId, String modelName, ModelType modelType, String artifactPath) {
        UserEntity user = userRepository.getReferenceById(userId);
        AiModelEntity aiModel = AiModelEntity.create(user, modelName, modelType, artifactPath);

        return aiModelRepository.save(aiModel);
    }

}
