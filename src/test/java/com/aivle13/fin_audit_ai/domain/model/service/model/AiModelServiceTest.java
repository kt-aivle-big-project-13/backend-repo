package com.aivle13.fin_audit_ai.domain.model.service.model;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelStatus;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.global.exception.model.core.DuplicateModelNameException;
import com.aivle13.fin_audit_ai.global.exception.model.core.ModelNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiModelServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PREVIOUS_MODEL_ID = 10L;

    @Mock
    private AiModelRepository aiModelRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserEntity user;

    @InjectMocks
    private AiModelService aiModelService;

    @Test
    void previousModelId가_없으면_새로운_modelGroupId를_발급한다() {
        given(userRepository.getReferenceById(USER_ID)).willReturn(user);
        given(aiModelRepository.save(any(AiModelEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        AiModelEntity saved = aiModelService.create(
                USER_ID, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                "models/model.json", "model.json", "1.0.0", null
        );

        assertThatCode(() -> UUID.fromString(saved.getModelGroupId())).doesNotThrowAnyException();
    }

    @Test
    void previousModelId가_있으면_해당_모델의_modelGroupId를_이어받는다() {
        AiModelEntity previousModel = AiModelEntity.create(
                user, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                "models/model-v1.json", "1.0.0"
        );
        given(userRepository.getReferenceById(USER_ID)).willReturn(user);
        given(aiModelRepository.findByIdAndUser_Id(PREVIOUS_MODEL_ID, USER_ID))
                .willReturn(Optional.of(previousModel));
        given(aiModelRepository.save(any(AiModelEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        AiModelEntity saved = aiModelService.create(
                USER_ID, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                "models/model-v2.json", "model-v2.json", "2.0.0", PREVIOUS_MODEL_ID
        );

        assertThat(saved.getModelGroupId()).isEqualTo(previousModel.getModelGroupId());
    }

    @Test
    void 신규_등록시_같은_사용자의_모델명이_이미_있으면_예외가_발생한다() {
        given(aiModelRepository.existsByUser_IdAndModelNameAndStatus(USER_ID, "credit-model", ModelStatus.ACTIVE))
                .willReturn(true);

        assertThatThrownBy(() ->
                aiModelService.create(
                        USER_ID, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                        "models/model.json", "model.json", "1.0.0", null
                )
        ).isInstanceOf(DuplicateModelNameException.class);
    }

    // existsByUser_IdAndModelName 검증과 save 사이에 동시 요청이 끼어들지 못하도록,
    // 신규 등록 시 (userId, modelName) advisory lock을 검증보다 먼저 획득해야 한다.
    @Test
    void 신규_등록시_중복_검증_전에_advisory_lock을_먼저_획득한다() {
        given(userRepository.getReferenceById(USER_ID)).willReturn(user);
        given(aiModelRepository.save(any(AiModelEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        aiModelService.create(
                USER_ID, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                "models/model.json", "model.json", "1.0.0", null
        );

        InOrder inOrder = inOrder(aiModelRepository);
        inOrder.verify(aiModelRepository).lockForModelNameRegistration(anyLong());
        inOrder.verify(aiModelRepository).existsByUser_IdAndModelNameAndStatus(USER_ID, "credit-model", ModelStatus.ACTIVE);
        inOrder.verify(aiModelRepository).save(any(AiModelEntity.class));
    }

    @Test
    void 버전업시에는_모델명이_같아도_예외가_발생하지_않는다() {
        AiModelEntity previousModel = AiModelEntity.create(
                user, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                "models/model-v1.json", "1.0.0"
        );
        given(userRepository.getReferenceById(USER_ID)).willReturn(user);
        given(aiModelRepository.findByIdAndUser_Id(PREVIOUS_MODEL_ID, USER_ID))
                .willReturn(Optional.of(previousModel));
        given(aiModelRepository.save(any(AiModelEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() ->
                aiModelService.create(
                        USER_ID, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                        "models/model-v2.json", "model-v2.json", "2.0.0", PREVIOUS_MODEL_ID
                )
        ).doesNotThrowAnyException();

        verify(aiModelRepository, never()).lockForModelNameRegistration(anyLong());
        verify(aiModelRepository, never()).existsByUser_IdAndModelNameAndStatus(any(), any(), any());
    }

    @Test
    void previousModelId에_해당하는_모델이_없으면_예외가_발생한다() {
        given(userRepository.getReferenceById(USER_ID)).willReturn(user);
        given(aiModelRepository.findByIdAndUser_Id(PREVIOUS_MODEL_ID, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                aiModelService.create(
                        USER_ID, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                        "models/model-v2.json", "model-v2.json", "2.0.0", PREVIOUS_MODEL_ID
                )
        ).isInstanceOf(ModelNotFoundException.class);
    }
}