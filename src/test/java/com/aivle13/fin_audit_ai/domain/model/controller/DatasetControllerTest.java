package com.aivle13.fin_audit_ai.domain.model.controller;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
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
class DatasetControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiModelRepository aiModelRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    private Long userId;
    private Long modelId;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.create("테스트기관", "홍길동", "dataset-test@example.com", "hash", UserRole.AUDITOR);
        userId = userRepository.save(user).getId();

        AiModelEntity model = AiModelEntity.create(
                user, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING, "models/model-key.pkl", "1.0.0"
        );
        modelId = aiModelRepository.save(model).getId();

        when(fileStorageService.store(any(), anyString()))
                .thenReturn(new StoredFile("datasets/test-key.csv", "data.csv", "text/csv", 100L));
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private MockMultipartFile datasetFile(String content) {
        return new MockMultipartFile("datasetFile", "audit_dataset.csv", "text/csv", content.getBytes());
    }

    @Test
    @DisplayName("감사 데이터셋을 업로드하면 201과 함께 rowCount/columns가 반환된다")
    void upload_success() throws Exception {
        MockMultipartFile file = datasetFile("age,income,gender,default\n30,5000,M,0\n40,6000,F,1\n");

        mockMvc.perform(multipart("/api/models/" + modelId + "/datasets")
                        .file(file)
                        .param("dataSource", "CUSTOMER")
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modelId").value(modelId))
                .andExpect(jsonPath("$.dataSource").value("CUSTOMER"))
                .andExpect(jsonPath("$.purpose").value("AUDIT"))
                .andExpect(jsonPath("$.rowCount").value(2))
                .andExpect(jsonPath("$.columns[0]").value("age"))
                .andExpect(jsonPath("$.columns[1]").value("income"))
                .andExpect(jsonPath("$.columns[2]").value("gender"))
                .andExpect(jsonPath("$.columns[3]").value("default"));
    }

    @Test
    @DisplayName("purpose를 지정하지 않으면 AUDIT로 저장된다")
    void upload_defaultsPurposeToAudit() throws Exception {
        MockMultipartFile file = datasetFile("age,income\n30,5000\n");

        mockMvc.perform(multipart("/api/models/" + modelId + "/datasets")
                        .file(file)
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purpose").value("AUDIT"));
    }

    @Test
    @DisplayName("purpose=VALIDATION으로 업로드하면 검증용 데이터셋으로 저장된다")
    void upload_validationPurpose() throws Exception {
        MockMultipartFile file = datasetFile("age,income\n30,5000\n");

        mockMvc.perform(multipart("/api/models/" + modelId + "/datasets")
                        .file(file)
                        .param("purpose", "VALIDATION")
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purpose").value("VALIDATION"));
    }

    @Test
    @DisplayName("존재하지 않는 모델이면 404를 반환한다")
    void upload_modelNotFound() throws Exception {
        MockMultipartFile file = datasetFile("age,income\n30,5000\n");

        mockMvc.perform(multipart("/api/models/999999/datasets")
                        .file(file)
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void upload_unauthorized() throws Exception {
        MockMultipartFile file = datasetFile("age,income\n30,5000\n");

        mockMvc.perform(multipart("/api/models/" + modelId + "/datasets")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }
}
