package com.aivle13.fin_audit_ai.domain.board.controller;

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
// 댓글 작성/수정/삭제는 ROLE_ADMIN 전용이므로 admin/일반 사용자 두 계정을 모두 준비해 둔다.
@AutoConfigureMockMvc
@Transactional
class CommentControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private Long userId;
    private Long adminId;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.create("테스트기관", "홍길동", "comment-test@example.com", "hash", UserRole.AUDITOR);
        userId = userRepository.save(user).getId();

        UserEntity admin = UserEntity.create("테스트기관", "관리자", "comment-admin@example.com", "hash", UserRole.ADMIN);
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
    @DisplayName("게시글의 댓글 목록을 작성 순서대로 조회한다")
    void list_success() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("존재하지 않는 게시글의 댓글을 조회하면 404를 반환한다")
    void list_postNotFound() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("관리자가 댓글을 작성하면 201을 반환한다")
    void create_byAdmin_success() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("관리자가 아니면 댓글 작성 시 403을 반환한다")
    void create_forbidden() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("관리자가 댓글을 수정한다")
    void update_byAdmin_success() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("존재하지 않는 댓글을 수정하면 404를 반환한다")
    void update_notFound() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("관리자가 댓글을 삭제하면 204를 반환한다")
    void delete_byAdmin_success() {
    }
}
