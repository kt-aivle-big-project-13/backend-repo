package com.aivle13.fin_audit_ai.domain.board.controller;

import com.aivle13.fin_audit_ai.domain.board.entity.CommentEntity;
import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;
import com.aivle13.fin_audit_ai.domain.board.repository.CommentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostRepository;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 댓글 작성/수정/삭제는 SecurityConfig에서 ROLE_ADMIN으로 제한되어 있다.
@AutoConfigureMockMvc
@Transactional
class CommentControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    private UserEntity user;
    private UserEntity admin;
    private PostEntity post;

    @BeforeEach
    void setUp() {
        user = userRepository.save(
                UserEntity.create("홍길동", "테스트기관", "comment-test@example.com", "hash", UserRole.AUDITOR));
        admin = userRepository.save(
                UserEntity.create("관리자", "테스트기관", "comment-admin@example.com", "hash", UserRole.ADMIN));
        post = postRepository.save(PostEntity.create(user, "댓글 테스트용 게시글", "본문"));
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(user.getId(), null, List.of());
    }

    private Authentication asAdmin() {
        return new UsernamePasswordAuthenticationToken(
                admin.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private CommentEntity createComment(String content) {
        return commentRepository.save(CommentEntity.create(post, admin, content));
    }

    @Test
    @DisplayName("게시글의 댓글 목록을 작성 순서대로 조회한다")
    void list_success() throws Exception {
        createComment("첫 번째 답변");
        createComment("두 번째 답변");

        mockMvc.perform(get("/api/v1/posts/{postId}/comments", post.getId())
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("첫 번째 답변"))
                .andExpect(jsonPath("$[1].content").value("두 번째 답변"));
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 댓글을 조회하면 404를 반환한다")
    void list_postNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{postId}/comments", 999_999L)
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("관리자가 댓글을 작성하면 201을 반환한다")
    void create_byAdmin_success() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{postId}/comments", post.getId())
                        .contentType("application/json")
                        .content("{\"content\": \"관리자 답변입니다\"}")
                        .with(authentication(asAdmin())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("관리자 답변입니다"))
                .andExpect(jsonPath("$.authorName").value("관리자"));
    }

    @Test
    @DisplayName("관리자가 아니면 댓글 작성 시 403을 반환한다")
    void create_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{postId}/comments", post.getId())
                        .contentType("application/json")
                        .content("{\"content\": \"일반 사용자 시도\"}")
                        .with(authentication(asUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자가 댓글을 수정한다")
    void update_byAdmin_success() throws Exception {
        CommentEntity comment = createComment("수정 전 내용");

        mockMvc.perform(patch("/api/v1/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                        .contentType("application/json")
                        .content("{\"content\": \"수정 후 내용\"}")
                        .with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정 후 내용"));
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 수정하면 404를 반환한다")
    void update_notFound() throws Exception {
        mockMvc.perform(patch("/api/v1/posts/{postId}/comments/{commentId}", post.getId(), 999_999L)
                        .contentType("application/json")
                        .content("{\"content\": \"수정 시도\"}")
                        .with(authentication(asAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("관리자가 댓글을 삭제하면 204를 반환한다")
    void delete_byAdmin_success() throws Exception {
        CommentEntity comment = createComment("삭제될 댓글");

        mockMvc.perform(delete("/api/v1/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                        .with(authentication(asAdmin())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/posts/{postId}/comments", post.getId())
                        .with(authentication(asUser())))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
