package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.ThresholdMethod;
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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Autowired
    private AuditRepository auditRepository;

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

    // 임계값 관련이 아닌 테스트용 기본값(MANUAL + 0.5)
    private String requestJson(Long modelId, Long datasetId, Long assessmentId, String auditName) {
        return requestJson(modelId, datasetId, assessmentId, auditName, "MANUAL", null, "0.5");
    }

    private String requestJson(Long modelId, Long datasetId, Long assessmentId, String auditName,
                                String thresholdMethod, String targetApprovalRate, String manualThreshold) {
        return requestJson(modelId, datasetId, assessmentId, auditName, thresholdMethod, targetApprovalRate, manualThreshold, null);
    }

    private String requestJson(Long modelId, Long datasetId, Long assessmentId, String auditName,
                                String thresholdMethod, String targetApprovalRate, String manualThreshold,
                                Long validationDatasetId) {
        return "{\"modelId\": " + modelId + ", \"datasetId\": " + datasetId
                + ", \"assessmentId\": " + assessmentId + ", \"auditName\": \"" + auditName + "\""
                + ", \"thresholdMethod\": " + (thresholdMethod == null ? "null" : "\"" + thresholdMethod + "\"")
                + ", \"targetApprovalRate\": " + targetApprovalRate
                + ", \"manualThreshold\": " + manualThreshold
                + ", \"validationDatasetId\": " + validationDatasetId + "}";
    }

    @Test
    @DisplayName("모델/데이터셋/민감정보가 다 갖춰지면 202와 함께 PENDING 상태로 감사가 생성된다")
    void start_success() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.auditId").exists())
                .andExpect(jsonPath("$.startedAt").exists());
    }

    @Test
    @DisplayName("존재하지 않는 모델이면 404를 반환한다")
    void start_modelNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(999999L, datasetId, null, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하지 않는 데이터셋이면 404를 반환한다")
    void start_datasetNotFound() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/v1/audits")
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

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(otherModelId, datasetId, null, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("민감정보를 선택하지 않았으면 400을 반환한다")
    void start_sensitiveAttributesNotSelected() throws Exception {
        mockMvc.perform(post("/api/v1/audits")
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

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, otherDatasetId, null, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("감사가 시작되면 데이터셋이 잠겨 이후 민감정보 수정 API가 409를 반환한다")
    void start_locksDatasetSensitiveAttributes() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isAccepted());

        mockMvc.perform(patch("/api/models/" + modelId + "/datasets/" + datasetId + "/sensitive-attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensitiveAttributes\": [\"age\"]}")
                        .with(authentication(asUser())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("동일 모델의 감사가 이미 진행 중이면 409를 반환한다")
    void start_alreadyInProgress() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "2차 감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void start_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("임계값 설정 방식이 없으면 400을 반환한다")
    void start_thresholdMethodMissing() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사", null, null, "0.5"))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("VALIDATION_DATASET 방식을 골라도 목표 승인율 없이 감사가 시작된다 (AI 서버 기본값 0.90 사용)")
    void start_validationDatasetMethod_withoutTargetApprovalRate_succeeds() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사", "VALIDATION_DATASET", null, null))
                        .with(authentication(asUser())))
                .andExpect(status().isAccepted());

        AuditEntity saved = auditRepository.findAll().get(0);
        assertThat(saved.getThresholdMethod()).isEqualTo(ThresholdMethod.VALIDATION_DATASET);
        assertThat(saved.getTargetApprovalRate()).isNull();
    }

    @Test
    @DisplayName("목표 승인율이 저장된다")
    void start_savesTargetApprovalRate() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사",
                                "VALIDATION_DATASET", "0.8", null))
                        .with(authentication(asUser())))
                .andExpect(status().isAccepted());

        AuditEntity saved = auditRepository.findAll().get(0);
        assertThat(saved.getThresholdMethod()).isEqualTo(ThresholdMethod.VALIDATION_DATASET);
        assertThat(saved.getTargetApprovalRate()).isEqualByComparingTo(BigDecimal.valueOf(0.8));
    }

    @Test
    @DisplayName("목표 승인율이 0 이하이면 400을 반환한다")
    void start_targetApprovalRate_tooLow() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사",
                                "VALIDATION_DATASET", "0", null))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("목표 승인율이 1 이상이면 400을 반환한다")
    void start_targetApprovalRate_tooHigh() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사",
                                "VALIDATION_DATASET", "1", null))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("검증용(VALIDATION) 데이터셋을 감사 데이터셋으로 지정하면 404를 반환한다")
    void start_datasetIsValidationPurpose() throws Exception {
        AiModelEntity model = aiModelRepository.findById(modelId).orElseThrow();
        DatasetEntity validationDataset = DatasetEntity.create(model, DataSource.CUSTOMER, "datasets/valid-key.csv", 50, "age,gender,income");
        validationDataset.markAsValidation();
        Long validationDatasetId = datasetRepository.save(validationDataset).getId();

        mockMvc.perform(post("/api/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, validationDatasetId, null, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("수동 임계값이 범위(0~1)를 벗어나면 400을 반환한다")
    void start_manualThreshold_outOfRange() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사",
                                "MANUAL", null, "1.5"))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("수동 임계값이 0이면 400을 반환한다 (아무도 승인되지 않아 AI 서버가 거부하는 값)")
    void start_manualThreshold_zero() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사",
                                "MANUAL", null, "0"))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("VALIDATION_DATASET 방식에 수동 임계값을 함께 보내면 400을 반환한다")
    void start_validationDatasetMethod_withManualThreshold() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사",
                                "VALIDATION_DATASET", "0.8", "0.5"))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("MANUAL 방식인데 수동 임계값이 없으면 400을 반환한다")
    void start_manualMethod_withoutManualThreshold() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사",
                                "MANUAL", null, null))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("MANUAL 방식에 목표 승인율을 함께 보내면 400을 반환한다")
    void start_manualMethod_withTargetApprovalRate() throws Exception {
        selectSensitiveAttributes();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사",
                                "MANUAL", "0.8", "0.5"))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("같은 모델 계열의 다른 버전에 올린 데이터셋으로도 감사를 시작할 수 있다")
    void start_reusesDatasetFromOtherVersionInSameGroup() throws Exception {
        AiModelEntity currentModel = aiModelRepository.findById(modelId).orElseThrow();
        AiModelEntity previousVersion = aiModelRepository.save(AiModelEntity.create(
                currentModel.getUser(), "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                "models/model-v1.pkl", "0.9.0", currentModel.getModelGroupId()
        ));
        DatasetEntity reusedDataset = DatasetEntity.create(
                previousVersion, DataSource.CUSTOMER, "datasets/v1-key.csv", 100, "age,gender,income"
        );
        reusedDataset.updateSensitiveAttributes("gender");
        Long reusedDatasetId = datasetRepository.save(reusedDataset).getId();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, reusedDatasetId, null, "1차 정기감사"))
                        .with(authentication(asUser())))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("VALIDATION_DATASET 방식이고 validationDatasetId를 지정하지 않으면 계열 내 최신 검증 데이터셋이 자동 선택되어 저장된다")
    void start_autoSelectsLatestValidationDataset() throws Exception {
        selectSensitiveAttributes();
        AiModelEntity currentModel = aiModelRepository.findById(modelId).orElseThrow();
        DatasetEntity validationDataset = DatasetEntity.create(
                currentModel, DataSource.CUSTOMER, "datasets/valid-key.csv", 50, "age,gender,income"
        );
        validationDataset.markAsValidation();
        Long validationDatasetId = datasetRepository.save(validationDataset).getId();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사",
                                "VALIDATION_DATASET", "0.9", null))
                        .with(authentication(asUser())))
                .andExpect(status().isAccepted());

        List<AuditEntity> audits = auditRepository.findAll();
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).getValidationDataset().getId()).isEqualTo(validationDatasetId);
    }

    @Test
    @DisplayName("validationDatasetId를 직접 지정하면 자동 선택 대신 그 데이터셋이 저장된다")
    void start_usesExplicitlySpecifiedValidationDataset() throws Exception {
        selectSensitiveAttributes();
        AiModelEntity currentModel = aiModelRepository.findById(modelId).orElseThrow();

        DatasetEntity olderValidationDataset = DatasetEntity.create(
                currentModel, DataSource.CUSTOMER, "datasets/valid-old-key.csv", 50, "age,gender,income"
        );
        olderValidationDataset.markAsValidation();
        datasetRepository.save(olderValidationDataset);

        DatasetEntity chosenValidationDataset = DatasetEntity.create(
                currentModel, DataSource.CUSTOMER, "datasets/valid-chosen-key.csv", 50, "age,gender,income"
        );
        chosenValidationDataset.markAsValidation();
        Long chosenValidationDatasetId = datasetRepository.save(chosenValidationDataset).getId();

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(modelId, datasetId, null, "1차 정기감사",
                                "VALIDATION_DATASET", "0.9", null, chosenValidationDatasetId))
                        .with(authentication(asUser())))
                .andExpect(status().isAccepted());

        List<AuditEntity> audits = auditRepository.findAll();
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).getValidationDataset().getId()).isEqualTo(chosenValidationDatasetId);
    }

    @Test
    @DisplayName("감사 목록을 최신순으로 조회한다")
    void list_returnsAuditsOrderedByLatest() throws Exception {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        AiModelEntity model = aiModelRepository.findById(modelId).orElseThrow();
        DatasetEntity dataset = datasetRepository.findById(datasetId).orElseThrow();

        AuditEntity firstAudit = AuditEntity.create(model, dataset, user, "1차 정기감사", "age,gender",
                null, ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);
        auditRepository.save(firstAudit);

        AuditEntity secondAudit = AuditEntity.create(model, dataset, user, "2차 정기감사", "age,gender",
                null, ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);
        auditRepository.save(secondAudit);

        mockMvc.perform(get("/api/v1/audits").with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].auditId").value(secondAudit.getId()))
                .andExpect(jsonPath("$[0].modelName").value("credit-model"))
                .andExpect(jsonPath("$[1].auditId").value(firstAudit.getId()));
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void list_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/audits"))
                .andExpect(status().isUnauthorized());
    }
}
