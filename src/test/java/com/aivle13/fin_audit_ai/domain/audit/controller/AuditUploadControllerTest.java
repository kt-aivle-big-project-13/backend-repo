package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.file.dto.StoredFile;
import com.aivle13.fin_audit_ai.domain.file.service.FileStorageService;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class AuditUploadControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    // S3까지 실제로 붙지 않도록 파일 저장은 목으로 대체
    @MockitoBean
    private FileStorageService fileStorageService;

    private Long userId;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.create("테스트기관", "홍길동", "audit-test@example.com", "hash", UserRole.AUDITOR);
        userId = userRepository.save(user).getId();

        when(fileStorageService.store(any(), anyString()))
                .thenReturn(new StoredFile("models/test-key", "credit_model.json", "application/json", 1024L));
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private MockMultipartFile modelFile() {
        return new MockMultipartFile("modelFile", "credit_model.json", "application/json", "{}".getBytes());
    }

    private MockMultipartFile datasetFile() {
        return new MockMultipartFile("auditDatasetFile", "audit_dataset.csv", "text/csv", "gender,age,default\nM,30,0".getBytes());
    }

    @Test
    @DisplayName("필수 파일과 필수값을 채워 업로드하면 201과 함께 감사가 생성된다")
    void upload_success() throws Exception {
        mockMvc.perform(multipart("/api/audits")
                        .file(modelFile())
                        .file(datasetFile())
                        .param("auditName", "1차 정기감사")
                        .param("modelName", "credit-model")
                        .param("modelType", ModelType.XGBOOST.name())
                        .param("sensitiveFeatures", "gender,age")
                        .param("targetApprovalRate", "0.7")
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.uploadedFiles.model").value(true))
                .andExpect(jsonPath("$.uploadedFiles.auditDataset").value(true))
                .andExpect(jsonPath("$.uploadedFiles.validationDataset").value(false));
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void upload_unauthorized() throws Exception {
        mockMvc.perform(multipart("/api/audits")
                        .file(modelFile())
                        .file(datasetFile())
                        .param("auditName", "1차 정기감사")
                        .param("modelName", "credit-model")
                        .param("modelType", ModelType.XGBOOST.name())
                        .param("sensitiveFeatures", "gender,age"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("필수 파일(auditDatasetFile) 없이 요청하면 400을 반환한다")
    void upload_missingRequiredFile() throws Exception {
        mockMvc.perform(multipart("/api/audits")
                        .file(modelFile())
                        .param("auditName", "1차 정기감사")
                        .param("modelName", "credit-model")
                        .param("modelType", ModelType.XGBOOST.name())
                        .param("sensitiveFeatures", "gender,age")
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("필수 텍스트값(auditName)이 없으면 400을 반환한다")
    void upload_missingRequiredField() throws Exception {
        mockMvc.perform(multipart("/api/audits")
                        .file(modelFile())
                        .file(datasetFile())
                        .param("modelName", "credit-model")
                        .param("modelType", ModelType.XGBOOST.name())
                        .param("sensitiveFeatures", "gender,age")
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("임계값이 범위를 벗어나면 400을 반환한다")
    void upload_invalidThreshold() throws Exception {
        mockMvc.perform(multipart("/api/audits")
                        .file(modelFile())
                        .file(datasetFile())
                        .param("auditName", "1차 정기감사")
                        .param("modelName", "credit-model")
                        .param("modelType", ModelType.XGBOOST.name())
                        .param("sensitiveFeatures", "gender,age")
                        .param("threshold", "1.5")
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }
}
