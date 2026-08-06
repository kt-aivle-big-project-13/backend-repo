package com.aivle13.fin_audit_ai.domain.dashboard.controller;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.core.ThresholdMethod;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class DashboardControllerTest extends IntegrationTestSupport {

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

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.create("홍길동", "테스트기관", "dashboard-test@example.com", "hash", UserRole.AUDITOR);
        userId = userRepository.save(user).getId();
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    @Test
    @DisplayName("로그인한 사용자는 본인 소유 감사 통계 대시보드를 조회한다")
    void getDashboard_success() throws Exception {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        AiModelEntity model = aiModelRepository.save(AiModelEntity.create(
                user, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING, "models/key.pkl", "1.0.0"));
        DatasetEntity dataset = datasetRepository.save(DatasetEntity.create(
                model, DataSource.CUSTOMER, "datasets/key.csv", 100, "age,gender,income"));

        AuditEntity audit = AuditEntity.create(model, dataset, user, "1차 정기감사", "age,gender",
                null, ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);
        audit.complete(3, AuditStatus.COMPLIANT);
        auditRepository.save(audit);

        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.analyzedModelCount").value(1))
                .andExpect(jsonPath("$.summary.normalModelCount").value(1))
                .andExpect(jsonPath("$.summary.complianceRate").value(100.0))
                .andExpect(jsonPath("$.auditResultDistribution.totalCount").value(1))
                .andExpect(jsonPath("$.auditResultDistribution.normalCount").value(1))
                .andExpect(jsonPath("$.recentAudits.length()").value(1))
                .andExpect(jsonPath("$.recentAudits[0].modelName").value("credit-model"))
                .andExpect(jsonPath("$.fairnessMetricDistributions").isArray());
    }

    @Test
    @DisplayName("데이터가 없어도 빈 통계로 200을 반환한다")
    void getDashboard_empty() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.analyzedModelCount").value(0))
                .andExpect(jsonPath("$.summary.complianceRate").value(0.0))
                .andExpect(jsonPath("$.reviewRequiredTopModels").isEmpty())
                .andExpect(jsonPath("$.recentAudits").isEmpty());
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void getDashboard_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("다른 사용자의 감사 내역은 내 대시보드에 포함되지 않는다")
    void getDashboard_excludesOtherUsersAudits() throws Exception {
        UserEntity me = userRepository.findById(userId).orElseThrow();
        AiModelEntity myModel = aiModelRepository.save(AiModelEntity.create(
                me, "my-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING, "models/mine.pkl", "1.0.0"));
        DatasetEntity myDataset = datasetRepository.save(DatasetEntity.create(
                myModel, DataSource.CUSTOMER, "datasets/mine.csv", 100, "age,gender,income"));
        AuditEntity myAudit = AuditEntity.create(myModel, myDataset, me, "내 감사", "age,gender",
                null, ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);
        myAudit.complete(3, AuditStatus.COMPLIANT);
        auditRepository.save(myAudit);

        UserEntity other = userRepository.save(
                UserEntity.create("다른사람", "다른기관", "dashboard-other@example.com", "hash", UserRole.AUDITOR));
        AiModelEntity otherModel = aiModelRepository.save(AiModelEntity.create(
                other, "other-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING, "models/other.pkl", "1.0.0"));
        DatasetEntity otherDataset = datasetRepository.save(DatasetEntity.create(
                otherModel, DataSource.CUSTOMER, "datasets/other.csv", 100, "age,gender,income"));
        AuditEntity otherAudit = AuditEntity.create(otherModel, otherDataset, other, "다른 사람 감사", "age,gender",
                null, ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);
        otherAudit.complete(3, AuditStatus.NON_COMPLIANT);
        auditRepository.save(otherAudit);

        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.analyzedModelCount").value(1))
                .andExpect(jsonPath("$.summary.normalModelCount").value(1))
                .andExpect(jsonPath("$.summary.thresholdExceededCount").value(0))
                .andExpect(jsonPath("$.recentAudits.length()").value(1))
                .andExpect(jsonPath("$.recentAudits[0].modelName").value("my-model"));
    }
}
