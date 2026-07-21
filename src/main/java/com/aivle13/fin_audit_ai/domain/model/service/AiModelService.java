package com.aivle13.fin_audit_ai.domain.model.service;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiModelService {

    private static final String DEFAULT_VERSION = "1.0.0";
    private static final ModelDomain DEFAULT_DOMAIN = ModelDomain.CREDIT_SCORING;

    private final AiModelRepository aiModelRepository;
    private final UserRepository userRepository;

    public AiModelEntity create(Long userId, String modelName, ModelType modelType, ModelDomain domain, String artifactPath, String version) {
        UserEntity user = userRepository.getReferenceById(userId);

        String resolvedVersion = StringUtils.hasText(version) ? version : DEFAULT_VERSION;
        ModelDomain resolvedDomain = domain != null ? domain : DEFAULT_DOMAIN;

        AiModelEntity aiModel = AiModelEntity.create(user, modelName, modelType, resolvedDomain, artifactPath, resolvedVersion);

        return aiModelRepository.save(aiModel);
    }

}
