package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class AuditControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiModelRepository aiModelRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    private Long userId;
    private Long modelId;
    private Long datasetId;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.create("테스트기관", "홍길동", "audit-start-test@example.com", "hash", UserRole.AUDITOR);
        userId = userRepository.save(user).getId();

        AiModelEntity model = AiModelEntity.create(
                user, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING, "models/model-key.pkl", "1.0.0"
        );
        modelId = aiModelRepository.save(model).getId();

        DatasetEntity dataset = DatasetEntity.create(model, DataSource.CUSTOMER, "datasets/test-key.csv", 100, "age,gender,income");
        datasetId = datasetRepository.save(dataset).getId();
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private void selectSensitiveAttributes() {
        DatasetEntity dataset = datasetRepository.findById(datasetId).orElseThrow();
        dataset.updateSensitiveAttributes("gender");
    }

    private String requestJson(Long modelId, Long datasetId, Long assessmentId, String auditName) {
        return "{\"modelId\": " + modelId + ", \"datasetId\": " + datasetId
                + ", \"assessmentId\": " + assessmentId + ", \"auditName\": \"" + auditName + "\"}";
    }

    @Test
    @DisplayName("모델/데이터셋/민감정보가 다 갖춰지면 202와 함께 PENDING 상태로 감사가 생성된다")
    void start_success() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, 5L, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.auditId").exists())
                .andExpect(jsonPath("$.startedAt").exists());
    }

    @Test
    @DisplayName("존재하지 않는 모델이면 404를 반환한다")
    void start_modelNotFound() throws Exception {
        mockMvc.perform(post("/api/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(999999L, datasetId, null, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하지 않는 데이터셋이면 404를 반환한다")
    void start_datasetNotFound() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, 999999L, null, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("다른 모델 소속 데이터셋을 참조하면 404를 반환한다")
    void start_datasetBelongsToOtherModel() throws Exception {
        selectSensitiveAttributes();

        AiModelEntity otherModel = AiModelEntity.create(
                userRepository.findById(userId).orElseThrow(), "other-model", ModelType.XGBOOST,
                ModelDomain.CREDIT_SCORING, "models/other-key.pkl", "1.0.0"
        );
        Long otherModelId = aiModelRepository.save(otherModel).getId();

        mockMvc.perform(post("/api/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(otherModelId, datasetId, null, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("민감정보를 선택하지 않았으면 400을 반환한다")
    void start_sensitiveAttributesNotSelected() throws Exception {
        mockMvc.perform(post("/api/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("같은 모델의 다른 데이터셋에서 선택한 민감정보는 적용되지 않아 400을 반환한다")
    void start_sensitiveAttributesNotSelectedForRequestedDataset() throws Exception {
        selectSensitiveAttributes();

        AiModelEntity model = aiModelRepository.findById(modelId).orElseThrow();
        DatasetEntity otherDataset = DatasetEntity.create(model, DataSource.CUSTOMER, "datasets/other-key.csv", 50, "age,gender,income");
        Long otherDatasetId = datasetRepository.save(otherDataset).getId();

        mockMvc.perform(post("/api/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, otherDatasetId, null, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("동일 모델의 감사가 이미 진행 중이면 409를 반환한다")
    void start_alreadyInProgress() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "2차 감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void start_unauthorized() throws Exception {
        mockMvc.perform(post("/api/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사")))
                .andExpect(status().isUnauthorized());
    }
}
