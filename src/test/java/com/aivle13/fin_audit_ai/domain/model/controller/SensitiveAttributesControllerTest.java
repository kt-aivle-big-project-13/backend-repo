package com.aivle13.fin_audit_ai.domain.model.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class SensitiveAttributesControllerTest extends IntegrationTestSupport {

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
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.create("테스트기관", "홍길동", "sensitive-test@example.com", "hash", UserRole.AUDITOR);
        userId = userRepository.save(user).getId();

        UserEntity otherUser = UserEntity.create("다른기관", "김철수", "other-user-test@example.com", "hash", UserRole.AUDITOR);
        otherUserId = userRepository.save(otherUser).getId();

        AiModelEntity model = AiModelEntity.create(
                user, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING, "models/model-key.pkl", "1.0.0"
        );
        modelId = aiModelRepository.save(model).getId();
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private Authentication asOtherUser() {
        return new UsernamePasswordAuthenticationToken(otherUserId, null, List.of());
    }

    private Long uploadDataset(String columns) {
        AiModelEntity model = aiModelRepository.findById(modelId).orElseThrow();
        DatasetEntity dataset = DatasetEntity.create(model, DataSource.CUSTOMER, "datasets/test-key.csv", 100, columns);
        return datasetRepository.save(dataset).getId();
    }

    @Test
    @DisplayName("데이터셋 컬럼에 있는 값이면 200과 함께 저장된다")
    void update_success() throws Exception {
        Long datasetId = uploadDataset("age,income,gender,default");

        mockMvc.perform(patch("/api/models/" + modelId + "/datasets/" + datasetId + "/sensitive-attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensitiveAttributes\": [\"gender\", \"age\"]}")
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetId").value(datasetId))
                .andExpect(jsonPath("$.sensitiveAttributes[0]").value("gender"))
                .andExpect(jsonPath("$.sensitiveAttributes[1]").value("age"));
    }

    @Test
    @DisplayName("데이터셋에 없는 컬럼이면 400을 반환한다")
    void update_invalidColumn() throws Exception {
        Long datasetId = uploadDataset("age,income,default");

        mockMvc.perform(patch("/api/models/" + modelId + "/datasets/" + datasetId + "/sensitive-attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensitiveAttributes\": [\"gender\"]}")
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 데이터셋이면 404를 반환한다")
    void update_datasetNotFound() throws Exception {
        mockMvc.perform(patch("/api/models/" + modelId + "/datasets/999999/sensitive-attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensitiveAttributes\": [\"gender\"]}")
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하지 않는 모델이면 404를 반환한다")
    void update_modelNotFound() throws Exception {
        Long datasetId = uploadDataset("age,income,gender");

        mockMvc.perform(patch("/api/models/999999/datasets/" + datasetId + "/sensitive-attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensitiveAttributes\": [\"gender\"]}")
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("빈 목록이면 400을 반환한다")
    void update_emptyList() throws Exception {
        Long datasetId = uploadDataset("age,income,gender");

        mockMvc.perform(patch("/api/models/" + modelId + "/datasets/" + datasetId + "/sensitive-attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensitiveAttributes\": []}")
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이미 감사에 사용된 데이터셋이면 409를 반환한다")
    void update_alreadyAudited() throws Exception {
        Long datasetId = uploadDataset("age,income,gender,default");
        DatasetEntity dataset = datasetRepository.findById(datasetId).orElseThrow();
        dataset.markAudited();

        mockMvc.perform(patch("/api/models/" + modelId + "/datasets/" + datasetId + "/sensitive-attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensitiveAttributes\": [\"gender\"]}")
                        .with(authentication(asUser())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("모델 소유자가 아닌 사용자가 요청하면 404를 반환한다")
    void update_notOwner() throws Exception {
        Long datasetId = uploadDataset("age,income,gender");

        mockMvc.perform(patch("/api/models/" + modelId + "/datasets/" + datasetId + "/sensitive-attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensitiveAttributes\": [\"gender\"]}")
                        .with(authentication(asOtherUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void update_unauthorized() throws Exception {
        Long datasetId = uploadDataset("age,income,gender");

        mockMvc.perform(patch("/api/models/" + modelId + "/datasets/" + datasetId + "/sensitive-attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensitiveAttributes\": [\"gender\"]}"))
                .andExpect(status().isUnauthorized());
    }
}
