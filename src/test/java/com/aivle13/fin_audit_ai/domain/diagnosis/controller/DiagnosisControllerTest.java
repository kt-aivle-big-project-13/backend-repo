package com.aivle13.fin_audit_ai.domain.diagnosis.controller;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// TODO(#252): board/dashboard/diagnosis 테스트 스켈레톤. @Disabled를 지우고 본문을 채운다.
@AutoConfigureMockMvc
@Transactional
class DiagnosisControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.create("테스트기관", "홍길동", "diagnosis-test@example.com", "hash", UserRole.AUDITOR);
        userId = userRepository.save(user).getId();
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("사전진단을 시작하면 201과 함께 IN_PROGRESS 상태로 생성된다")
    void start_success() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("인증 정보가 없으면 시작 시 401을 반환한다")
    void start_unauthorized() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("GATE 문항 중 하나라도 해당되면 HIGH_IMPACT로 전이한다")
    void diagnoseQualitative_highImpact() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("GATE 문항이 모두 해당 없음이면 정량 진단 필요 상태로 전이한다")
    void diagnoseQualitative_needsQuantitative() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("GATE 문항이 누락되면 400을 반환한다")
    void diagnoseQualitative_missingAnswer() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("IN_PROGRESS 상태가 아니면 정성 진단 제출 시 400을 반환한다")
    void diagnoseQualitative_invalidState() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("가중 점수 합이 임계값 이상이면 HIGH_IMPACT로 확정된다")
    void diagnoseQuantitative_highImpact() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("가중 점수 합이 임계값 미만이면 NOT_APPLICABLE로 확정된다")
    void diagnoseQuantitative_notApplicable() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("GATE 단계를 통과하지 않으면 정량 진단 제출 시 400을 반환한다")
    void diagnoseQuantitative_invalidState() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("사전진단 결과를 조회한다")
    void getResult_success() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("존재하지 않는 사전진단을 조회하면 404를 반환한다")
    void getResult_notFound() {
    }
}
