package com.aivle13.fin_audit_ai.domain.dashboard.controller;

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
class DashboardControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.create("테스트기관", "홍길동", "dashboard-test@example.com", "hash", UserRole.AUDITOR);
        userId = userRepository.save(user).getId();
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("로그인한 사용자는 전체 감사 통계 대시보드를 조회한다")
    void getDashboard_success() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("데이터가 없어도 빈 통계로 200을 반환한다")
    void getDashboard_empty() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void getDashboard_unauthorized() {
    }
}
