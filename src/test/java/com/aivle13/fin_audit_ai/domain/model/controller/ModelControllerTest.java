package com.aivle13.fin_audit_ai.domain.model.controller;

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
class ModelControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    private Long userId;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.create("테스트기관", "홍길동", "model-test@example.com", "hash", UserRole.AUDITOR);
        userId = userRepository.save(user).getId();

        when(fileStorageService.store(any(), anyString()))
                .thenReturn(new StoredFile("models/test-key", "credit_scoring_v2.pkl", "application/octet-stream", 2048L));
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    @Test
    @DisplayName("모델 파일을 업로드하면 201과 함께 모델이 등록된다")
    void upload_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "credit_scoring_v2.pkl", "application/octet-stream", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/models")
                        .file(file)
                        .param("modelName", "credit_scoring_v2")
                        .param("modelType", ModelType.XGBOOST.name())
                        .param("version", "2.0.0")
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modelName").value("credit_scoring_v2"))
                .andExpect(jsonPath("$.version").value("2.0.0"))
                .andExpect(jsonPath("$.fileName").value("credit_scoring_v2.pkl"));
    }

    @Test
    @DisplayName("version을 안 보내면 기본값 1.0.0으로 등록된다")
    void upload_defaultVersion() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "model.joblib", "application/octet-stream", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/models")
                        .file(file)
                        .param("modelName", "credit_scoring_v2")
                        .param("modelType", ModelType.XGBOOST.name())
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void upload_unauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "model.pkl", "application/octet-stream", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/models")
                        .file(file)
                        .param("modelName", "credit_scoring_v2")
                        .param("modelType", ModelType.XGBOOST.name()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("지원하지 않는 확장자면 400을 반환한다")
    void upload_invalidExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "model.txt", "text/plain", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/models")
                        .file(file)
                        .param("modelName", "credit_scoring_v2")
                        .param("modelType", ModelType.XGBOOST.name())
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("modelName이 없으면 400을 반환한다")
    void upload_missingModelName() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "model.pkl", "application/octet-stream", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/models")
                        .file(file)
                        .param("modelType", ModelType.XGBOOST.name())
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }
}
