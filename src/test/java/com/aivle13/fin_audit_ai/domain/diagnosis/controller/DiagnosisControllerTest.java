package com.aivle13.fin_audit_ai.domain.diagnosis.controller;

import com.aivle13.fin_audit_ai.domain.diagnosis.entity.PreDiagnosisEntity;
import com.aivle13.fin_audit_ai.domain.diagnosis.repository.PreDiagnosisRepository;
import com.aivle13.fin_audit_ai.domain.diagnosis.type.DiagnosisResult;
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

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class DiagnosisControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PreDiagnosisRepository preDiagnosisRepository;

    private UserEntity user;
    private Long userId;

    @BeforeEach
    void setUp() {
        user = UserEntity.create("홍길동", "테스트기관", "diagnosis-test@example.com", "hash", UserRole.AUDITOR);
        userId = userRepository.save(user).getId();
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private Long createDiagnosis(DiagnosisResult result) {
        UserEntity owner = userRepository.findById(userId).orElseThrow();
        return preDiagnosisRepository.save(PreDiagnosisEntity.create(owner, result)).getId();
    }

    @Test
    @DisplayName("사전진단을 시작하면 201과 함께 IN_PROGRESS 상태로 생성된다")
    void start_success() throws Exception {
        mockMvc.perform(post("/api/impact-assessments")
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    @DisplayName("인증 정보가 없으면 시작 시 401을 반환한다")
    void start_unauthorized() throws Exception {
        mockMvc.perform(post("/api/impact-assessments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GATE 문항 중 하나라도 해당되면 HIGH_IMPACT로 전이한다")
    void diagnoseQualitative_highImpact() throws Exception {
        Long assessmentId = createDiagnosis(DiagnosisResult.IN_PROGRESS);

        mockMvc.perform(post("/api/impact-assessments/{assessmentId}/stage1", assessmentId)
                        .contentType("application/json")
                        .content("""
                                {"answers": [
                                    {"questionCode": "GATE_01", "answer": true},
                                    {"questionCode": "GATE_02", "answer": false}
                                ]}
                                """)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditionMet").value(true))
                .andExpect(jsonPath("$.result").value("HIGH_IMPACT"));
    }

    @Test
    @DisplayName("GATE 문항이 모두 해당 없음이면 정량 진단 필요 상태로 전이한다")
    void diagnoseQualitative_needsQuantitative() throws Exception {
        Long assessmentId = createDiagnosis(DiagnosisResult.IN_PROGRESS);

        mockMvc.perform(post("/api/impact-assessments/{assessmentId}/stage1", assessmentId)
                        .contentType("application/json")
                        .content("""
                                {"answers": [
                                    {"questionCode": "GATE_01", "answer": false},
                                    {"questionCode": "GATE_02", "answer": false}
                                ]}
                                """)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditionMet").value(false))
                .andExpect(jsonPath("$.result").value("NEEDS_QUANTITATIVE"));
    }

    @Test
    @DisplayName("GATE 문항이 누락되면 400을 반환한다")
    void diagnoseQualitative_missingAnswer() throws Exception {
        Long assessmentId = createDiagnosis(DiagnosisResult.IN_PROGRESS);

        mockMvc.perform(post("/api/impact-assessments/{assessmentId}/stage1", assessmentId)
                        .contentType("application/json")
                        .content("""
                                {"answers": [
                                    {"questionCode": "GATE_01", "answer": true}
                                ]}
                                """)
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("IN_PROGRESS 상태가 아니면 정성 진단 제출 시 400을 반환한다")
    void diagnoseQualitative_invalidState() throws Exception {
        Long assessmentId = createDiagnosis(DiagnosisResult.HIGH_IMPACT);

        mockMvc.perform(post("/api/impact-assessments/{assessmentId}/stage1", assessmentId)
                        .contentType("application/json")
                        .content("""
                                {"answers": [
                                    {"questionCode": "GATE_01", "answer": true},
                                    {"questionCode": "GATE_02", "answer": false}
                                ]}
                                """)
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("가중 점수 합이 임계값 이상이면 HIGH_IMPACT로 확정된다")
    void diagnoseQuantitative_highImpact() throws Exception {
        Long assessmentId = createDiagnosis(DiagnosisResult.NEEDS_QUANTITATIVE);

        mockMvc.perform(post("/api/impact-assessments/{assessmentId}/stage2", assessmentId)
                        .contentType("application/json")
                        .content("""
                                {"answers": [
                                    {"questionCode": "A_01", "answer": true},
                                    {"questionCode": "A_02", "answer": true},
                                    {"questionCode": "A_03", "answer": false},
                                    {"questionCode": "B_01", "answer": false},
                                    {"questionCode": "B_02", "answer": false},
                                    {"questionCode": "B_03", "answer": false}
                                ]}
                                """)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(4))
                .andExpect(jsonPath("$.result").value("HIGH_IMPACT"));
    }

    @Test
    @DisplayName("가중 점수 합이 임계값 미만이면 NOT_APPLICABLE로 확정된다")
    void diagnoseQuantitative_notApplicable() throws Exception {
        Long assessmentId = createDiagnosis(DiagnosisResult.NEEDS_QUANTITATIVE);

        mockMvc.perform(post("/api/impact-assessments/{assessmentId}/stage2", assessmentId)
                        .contentType("application/json")
                        .content("""
                                {"answers": [
                                    {"questionCode": "A_01", "answer": true},
                                    {"questionCode": "A_02", "answer": false},
                                    {"questionCode": "A_03", "answer": false},
                                    {"questionCode": "B_01", "answer": false},
                                    {"questionCode": "B_02", "answer": false},
                                    {"questionCode": "B_03", "answer": false}
                                ]}
                                """)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(2))
                .andExpect(jsonPath("$.result").value("NOT_APPLICABLE"));
    }

    @Test
    @DisplayName("GATE 단계를 통과하지 않으면 정량 진단 제출 시 400을 반환한다")
    void diagnoseQuantitative_invalidState() throws Exception {
        Long assessmentId = createDiagnosis(DiagnosisResult.IN_PROGRESS);

        mockMvc.perform(post("/api/impact-assessments/{assessmentId}/stage2", assessmentId)
                        .contentType("application/json")
                        .content("""
                                {"answers": [
                                    {"questionCode": "A_01", "answer": true},
                                    {"questionCode": "A_02", "answer": false},
                                    {"questionCode": "A_03", "answer": false},
                                    {"questionCode": "B_01", "answer": false},
                                    {"questionCode": "B_02", "answer": false},
                                    {"questionCode": "B_03", "answer": false}
                                ]}
                                """)
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("사전진단 결과를 조회한다")
    void getResult_success() throws Exception {
        Long assessmentId = createDiagnosis(DiagnosisResult.HIGH_IMPACT);

        mockMvc.perform(get("/api/impact-assessments/{assessmentId}", assessmentId)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentId").value(assessmentId))
                .andExpect(jsonPath("$.result").value("HIGH_IMPACT"));
    }

    @Test
    @DisplayName("존재하지 않는 사전진단을 조회하면 404를 반환한다")
    void getResult_notFound() throws Exception {
        mockMvc.perform(get("/api/impact-assessments/{assessmentId}", 999_999L)
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }
}
