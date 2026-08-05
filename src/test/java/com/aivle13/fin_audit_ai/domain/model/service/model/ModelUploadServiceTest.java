package com.aivle13.fin_audit_ai.domain.model.service.model;

import com.aivle13.fin_audit_ai.domain.model.dto.request.model.ModelUploadRequest;
import com.aivle13.fin_audit_ai.domain.model.dto.response.model.ModelUploadResponse;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.exception.model.dataset.InvalidModelFileException;
import com.aivle13.fin_audit_ai.global.exception.model.core.ModelNotFoundException;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import com.aivle13.fin_audit_ai.global.s3.validator.AuditFileValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ModelUploadServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PREVIOUS_MODEL_ID = 10L;

    @Mock
    private AuditFileValidator fileValidator;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private AiModelService aiModelService;
    @Mock
    private AiModelRepository aiModelRepository;
    @Mock
    private UserEntity user;
    @Mock
    private MultipartFile file;

    @InjectMocks
    private ModelUploadService modelUploadService;

    private AiModelEntity savedModel() {
        return AiModelEntity.create(
                user, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                "models/model-v2.json", "model-v2.json", "2.0.0", "group-a"
        );
    }

    @Test
    void 파일을_업로드하면_새로_저장하고_모델을_생성한다() {
        given(file.isEmpty()).willReturn(false);
        given(fileStorageService.store(file, "models"))
                .willReturn(new StoredFile("models/model.json", "model.json", "application/json", 10L));
        given(aiModelService.create(
                USER_ID, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                "models/model.json", "model.json", "1.0.0", null
        )).willReturn(savedModel());

        ModelUploadRequest request = new ModelUploadRequest(
                file, "credit-model", ModelType.XGBOOST, "1.0.0", ModelDomain.CREDIT_SCORING, null
        );

        ModelUploadResponse response = modelUploadService.upload(USER_ID, request);

        assertThat(response.fileName()).isEqualTo("model.json");
        verify(fileValidator).validateModelArtifactFile(file);
        verify(aiModelRepository, never()).findByIdAndUser_Id(any(), any());
    }

    @Test
    void 파일이_없고_이전_모델이_있으면_그_모델의_파일을_재사용한다() {
        AiModelEntity previousModel = AiModelEntity.create(
                user, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                "models/model-v1.json", "model-v1.json", "1.0.0", "group-a"
        );
        given(aiModelRepository.findByIdAndUser_Id(PREVIOUS_MODEL_ID, USER_ID))
                .willReturn(Optional.of(previousModel));
        given(aiModelService.create(
                USER_ID, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                "models/model-v1.json", "model-v1.json", "2.0.0", PREVIOUS_MODEL_ID
        )).willReturn(savedModel());

        ModelUploadRequest request = new ModelUploadRequest(
                null, "credit-model", ModelType.XGBOOST, "2.0.0", ModelDomain.CREDIT_SCORING, PREVIOUS_MODEL_ID
        );

        ModelUploadResponse response = modelUploadService.upload(USER_ID, request);

        assertThat(response.fileName()).isEqualTo("model-v1.json");
        verify(fileValidator, never()).validateModelArtifactFile(any());
        verify(fileStorageService, never()).store(any(), any());
    }

    @Test
    void 파일도_없고_이전_모델도_없으면_예외가_발생한다() {
        ModelUploadRequest request = new ModelUploadRequest(
                null, "credit-model", ModelType.XGBOOST, "1.0.0", ModelDomain.CREDIT_SCORING, null
        );

        assertThatThrownBy(() -> modelUploadService.upload(USER_ID, request))
                .isInstanceOf(InvalidModelFileException.class);
    }

    @Test
    void 이전_모델이_소유자의_것이_아니면_예외가_발생한다() {
        given(aiModelRepository.findByIdAndUser_Id(PREVIOUS_MODEL_ID, USER_ID))
                .willReturn(Optional.empty());

        ModelUploadRequest request = new ModelUploadRequest(
                null, "credit-model", ModelType.XGBOOST, "2.0.0", ModelDomain.CREDIT_SCORING, PREVIOUS_MODEL_ID
        );

        assertThatThrownBy(() -> modelUploadService.upload(USER_ID, request))
                .isInstanceOf(ModelNotFoundException.class);
    }
}
