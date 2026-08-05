package com.aivle13.fin_audit_ai.domain.board;

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

// TODO(#252): 게시글 작성 → 조회 → 댓글 작성 흐름 통합 테스트 스켈레톤.
// TestContainers(Postgres+Redis)로 실제 DB에 저장/조회되는 것까지 검증한다 (컨트롤러 단위 목킹이 아님).
// 흐름: 1) 일반 사용자로 게시글 작성 → 2) 상세 조회로 저장 확인 → 3) 관리자로 댓글 작성 → 4) 댓글 목록에 반영 확인.
@AutoConfigureMockMvc
@Transactional
class BoardFlowIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private Long userId;
    private Long adminId;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.create("테스트기관", "홍길동", "board-flow-user@example.com", "hash", UserRole.AUDITOR);
        userId = userRepository.save(user).getId();

        UserEntity admin = UserEntity.create("테스트기관", "관리자", "board-flow-admin@example.com", "hash", UserRole.ADMIN);
        adminId = userRepository.save(admin).getId();
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private Authentication asAdmin() {
        return new UsernamePasswordAuthenticationToken(adminId, null, List.of());
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("게시글을 작성하고 조회한 뒤 관리자가 댓글을 달면 댓글 목록에 반영된다")
    void createPost_thenGet_thenAdminComments_flow() {
    }
}
