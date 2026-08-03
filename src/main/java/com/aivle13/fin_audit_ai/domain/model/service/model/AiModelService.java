package com.aivle13.fin_audit_ai.domain.model.service.model;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.global.exception.model.DuplicateModelNameException;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AiModelService {

    private static final String DEFAULT_VERSION = "1.0.0";
    private static final ModelDomain DEFAULT_DOMAIN = ModelDomain.CREDIT_SCORING;

    private final AiModelRepository aiModelRepository;
    private final UserRepository userRepository;

    public AiModelEntity create(Long userId, String modelName, ModelType modelType, ModelDomain domain,
                                String artifactPath, String originalFileName, String version, Long previousModelId) {
        // 기존 모델의 새 버전(previousModelId 있음)은 같은 모델명을 그대로 이어받는 게 정상이므로,
        // 완전히 새로운 모델을 등록할 때만 모델명 중복을 검증한다.
        if (previousModelId == null) {
            // existsByUser_IdAndModelName 검증과 save 사이에 동시 요청이 끼어들면 같은 이름으로
            // 두 모델 그룹이 동시에 생성될 수 있다. (userId, modelName) advisory lock으로 이 구간을
            // 직렬화해서, 뒤에 도착한 요청은 앞선 요청의 커밋 이후에야 존재 여부를 검증하게 만든다.
            aiModelRepository.lockForModelNameRegistration(modelNameLockKey(userId, modelName));

            if (aiModelRepository.existsByUser_IdAndModelName(userId, modelName)) {
                throw new DuplicateModelNameException();
            }
        }

        UserEntity user = userRepository.getReferenceById(userId);

        String resolvedVersion = StringUtils.hasText(version) ? version : DEFAULT_VERSION;
        ModelDomain resolvedDomain = domain != null ? domain : DEFAULT_DOMAIN;
        String modelGroupId = previousModelId != null
                ? findModelGroupId(userId, previousModelId)
                : java.util.UUID.randomUUID().toString();

        AiModelEntity aiModel = AiModelEntity.create(
                user, modelName, modelType, resolvedDomain, artifactPath,
                originalFileName, resolvedVersion, modelGroupId
        );

        return aiModelRepository.save(aiModel);
    }

    private long modelNameLockKey(Long userId, String modelName) {
        return Objects.hash(userId, modelName);
    }

    private String findModelGroupId(Long userId, Long previousModelId) {
        return aiModelRepository.findByIdAndUser_Id(previousModelId, userId)
                .orElseThrow(ModelNotFoundException::new)
                .getModelGroupId();
    }

}