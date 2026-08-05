package com.aivle13.fin_audit_ai.domain.board.controller;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// TODO(#252): board/dashboard/diagnosis 테스트 스켈레톤. @Disabled를 지우고 본문을 채운다.
@AutoConfigureMockMvc
@Transactional
class PostControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    private Long userId;
    private Long adminId;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.create("테스트기관", "홍길동", "post-test@example.com", "hash", UserRole.AUDITOR);
        userId = userRepository.save(user).getId();

        UserEntity admin = UserEntity.create("테스트기관", "관리자", "post-admin@example.com", "hash", UserRole.ADMIN);
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
    @DisplayName("게시글 목록을 페이지네이션·검색·정렬 조건으로 조회한다")
    void list_success() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("인증 정보가 없으면 목록 조회 시 401을 반환한다")
    void list_unauthorized() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("page/size 값이 올바르지 않으면 400을 반환한다")
    void list_invalidPaging() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("게시글 상세를 첨부파일 목록과 함께 조회한다")
    void get_success() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("존재하지 않는 게시글을 조회하면 404를 반환한다")
    void get_notFound() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("게시글을 작성하면 201과 함께 게시글이 생성된다")
    void create_success() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("첨부파일이 5개를 초과하면 409를 반환한다")
    void create_attachmentLimitExceeded() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("작성자 본인이 게시글을 수정한다")
    void update_byAuthor_success() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("작성자가 아니고 관리자도 아니면 수정 시 403을 반환한다")
    void update_forbidden() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("작성자 본인이 게시글을 삭제하면 204를 반환한다")
    void delete_byAuthor_success() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("관리자가 게시글을 공지로 고정한다")
    void pin_byAdmin_success() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("관리자가 아니면 공지 고정 시 403을 반환한다")
    void pin_forbidden() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    @DisplayName("첨부파일을 다운로드한다")
    void download_success() {
    }
}
