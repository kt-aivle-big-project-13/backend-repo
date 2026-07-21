package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.AuditUploadRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.AuditUploadResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.validator.ThresholdPolicyValidator;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.service.AiModelService;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.global.exception.file.EmptyFileException;
import com.aivle13.fin_audit_ai.global.exception.model.InvalidThresholdPolicyException;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import com.aivle13.fin_audit_ai.global.s3.validator.AuditFileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuditUploadServiceTest {

    @Mock
    private AuditFileValidator fileValidator;
    @Mock
    private ThresholdPolicyValidator thresholdValidator;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private AiModelService aiModelService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditUploadService auditUploadService;

    private static final Long USER_ID = 1L;

    private final MockMultipartFile modelFile =
            new MockMultipartFile("modelFile", "model.json", "application/json", "{}".getBytes());
    private final MockMultipartFile auditDatasetFile =
            new MockMultipartFile("auditDatasetFile", "audit.csv", "text/csv", "a,b\n1,2".getBytes());
    private final MockMultipartFile validationDatasetFile =
            new MockMultipartFile("validationDatasetFile", "validation.csv", "text/csv", "a,b\n1,2".getBytes());

    private AiModelEntity aiModel;
    private AuditEntity audit;

    @BeforeEach
    void setUp() {
        aiModel = AiModelEntity.create(null, "my-model", ModelType.XGBOOST, "models/model.json");
        audit = AuditEntity.create(aiModel, null, "datasets/audit.csv", "age,gender");
    }

    private AuditUploadRequest requestWith(MultipartFile validationDatasetFile) {
        return new AuditUploadRequest(
                modelFile, auditDatasetFile, validationDatasetFile,
                "audit-name", "my-model", ModelType.XGBOOST,
                0.7, 0.5,
                "age,gender"
        );
    }

    @Test
    void 필수_파일만_있으면_검증데이터셋_없이_업로드에_성공한다() {
        given(fileStorageService.store(modelFile, "models"))
                .willReturn(new StoredFile("models/model-key.json", "model.json", "application/json", 2));
        given(fileStorageService.store(auditDatasetFile, "datasets"))
                .willReturn(new StoredFile("datasets/audit-key.csv", "audit.csv", "text/csv", 7));
        given(aiModelService.create(eq(USER_ID), anyString(), any(), anyString())).willReturn(aiModel);
        given(auditService.create(eq(USER_ID), eq(aiModel), anyString(), anyString())).willReturn(audit);

        AuditUploadResponse response = auditUploadService.upload(USER_ID, requestWith(null));

        assertThat(response.uploadedFiles().model()).isTrue();
        assertThat(response.uploadedFiles().auditDataset()).isTrue();
        assertThat(response.uploadedFiles().validationDataset()).isFalse();
        assertThat(response.status()).isEqualTo(AuditStatus.IN_PROGRESS.name());
        verify(fileStorageService, times(2)).store(any(), anyString());
        verify(fileStorageService, never()).delete(anyString());
    }

    @Test
    void 검증데이터셋이_유효하면_함께_저장되고_응답에_반영된다() {
        given(fileStorageService.store(modelFile, "models"))
                .willReturn(new StoredFile("models/model-key.json", "model.json", "application/json", 2));
        given(fileStorageService.store(auditDatasetFile, "datasets"))
                .willReturn(new StoredFile("datasets/audit-key.csv", "audit.csv", "text/csv", 7));
        given(fileStorageService.store(validationDatasetFile, "datasets"))
                .willReturn(new StoredFile("datasets/validation-key.csv", "validation.csv", "text/csv", 7));
        given(aiModelService.create(eq(USER_ID), anyString(), any(), anyString())).willReturn(aiModel);
        given(auditService.create(eq(USER_ID), eq(aiModel), anyString(), anyString())).willReturn(audit);

        AuditUploadResponse response = auditUploadService.upload(USER_ID, requestWith(validationDatasetFile));

        assertThat(response.uploadedFiles().validationDataset()).isTrue();
        verify(fileStorageService, times(3)).store(any(), anyString());
    }

    @Test
    void 검증데이터셋이_유효하지_않으면_무시하고_기본_감사만_진행한다() {
        given(fileStorageService.store(modelFile, "models"))
                .willReturn(new StoredFile("models/model-key.json", "model.json", "application/json", 2));
        given(fileStorageService.store(auditDatasetFile, "datasets"))
                .willReturn(new StoredFile("datasets/audit-key.csv", "audit.csv", "text/csv", 7));
        lenient().doThrow(new EmptyFileException("검증 데이터 파일이 존재하지 않습니다."))
                .when(fileValidator).validateCsvFile(validationDatasetFile, "검증 데이터");
        given(aiModelService.create(eq(USER_ID), anyString(), any(), anyString())).willReturn(aiModel);
        given(auditService.create(eq(USER_ID), eq(aiModel), anyString(), anyString())).willReturn(audit);

        AuditUploadResponse response = auditUploadService.upload(USER_ID, requestWith(validationDatasetFile));

        assertThat(response.uploadedFiles().validationDataset()).isFalse();
        verify(fileStorageService, never()).store(validationDatasetFile, "datasets");
        verify(fileStorageService, times(2)).store(any(), anyString());
        verify(fileStorageService, never()).delete(anyString());
    }

    @Test
    void 모델파일_검증에_실패하면_저장을_시도하지_않는다() {
        doThrow(new EmptyFileException("모델 파일이 존재하지 않습니다."))
                .when(fileValidator).validateModelFile(modelFile);
        AuditUploadRequest request = requestWith(null);

        assertThatThrownBy(() -> auditUploadService.upload(USER_ID, request))
                .isInstanceOf(EmptyFileException.class);

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void 임계값_정책_위반이면_저장을_시도하지_않는다() {
        doThrow(new InvalidThresholdPolicyException("임계값은 0 초과 1 이하여야 합니다."))
                .when(thresholdValidator).validate(any());
        AuditUploadRequest request = requestWith(null);

        assertThatThrownBy(() -> auditUploadService.upload(USER_ID, request))
                .isInstanceOf(InvalidThresholdPolicyException.class);

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void 필수_파일_저장_중_실패하면_이미_저장된_파일을_정리하고_예외를_전파한다() {
        given(fileStorageService.store(modelFile, "models"))
                .willReturn(new StoredFile("models/model-key.json", "model.json", "application/json", 2));
        given(fileStorageService.store(auditDatasetFile, "datasets"))
                .willThrow(new RuntimeException("S3 업로드 실패"));
        AuditUploadRequest request = requestWith(null);

        assertThatThrownBy(() -> auditUploadService.upload(USER_ID, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 업로드 실패");

        verify(fileStorageService).delete("models/model-key.json");
    }

    @Test
    void AiModel_저장에_실패하면_필수_파일들을_모두_정리한다() {
        given(fileStorageService.store(modelFile, "models"))
                .willReturn(new StoredFile("models/model-key.json", "model.json", "application/json", 2));
        given(fileStorageService.store(auditDatasetFile, "datasets"))
                .willReturn(new StoredFile("datasets/audit-key.csv", "audit.csv", "text/csv", 7));
        given(aiModelService.create(eq(USER_ID), anyString(), any(), anyString()))
                .willThrow(new RuntimeException("DB 저장 실패"));
        AuditUploadRequest request = requestWith(null);

        assertThatThrownBy(() -> auditUploadService.upload(USER_ID, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 저장 실패");

        verify(fileStorageService).delete("models/model-key.json");
        verify(fileStorageService).delete("datasets/audit-key.csv");
    }
}
