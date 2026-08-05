package com.aivle13.fin_audit_ai.domain.board;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 게시글 작성 → 조회 → 댓글 작성 흐름 통합 테스트.
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
        UserEntity user = UserEntity.create("홍길동", "테스트기관", "board-flow-user@example.com", "hash", UserRole.AUDITOR);
        userId = userRepository.save(user).getId();

        UserEntity admin = UserEntity.create("관리자", "테스트기관", "board-flow-admin@example.com", "hash", UserRole.ADMIN);
        adminId = userRepository.save(admin).getId();
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private Authentication asAdmin() {
        return new UsernamePasswordAuthenticationToken(
                adminId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("게시글을 작성하고 조회한 뒤 관리자가 댓글을 달면 댓글 목록에 반영된다")
    void createPost_thenGet_thenAdminComments_flow() throws Exception {
        // 1) 일반 사용자가 게시글을 작성한다.
        MvcResult createResult = mockMvc.perform(multipart("/api/v1/posts")
                        .param("title", "통합 테스트 게시글")
                        .param("content", "통합 테스트 본문")
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andReturn();
        Long postId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.id", Long.class);

        // 2) 방금 작성한 게시글을 상세 조회해 실제로 저장됐는지 확인한다.
        mockMvc.perform(get("/api/v1/posts/{postId}", postId)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("통합 테스트 게시글"))
                .andExpect(jsonPath("$.content").value("통합 테스트 본문"))
                .andExpect(jsonPath("$.attachments").isEmpty());

        // 게시글 목록에도 반영됐는지 확인한다.
        mockMvc.perform(get("/api/v1/posts")
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(postId));

        // 3) 관리자가 해당 게시글에 댓글(답변)을 작성한다.
        mockMvc.perform(post("/api/v1/posts/{postId}/comments", postId)
                        .contentType("application/json")
                        .content("{\"content\": \"관리자 답변입니다\"}")
                        .with(authentication(asAdmin())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("관리자 답변입니다"));

        // 4) 댓글 목록에 방금 작성한 댓글이 반영된다.
        mockMvc.perform(get("/api/v1/posts/{postId}/comments", postId)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("관리자 답변입니다"))
                .andExpect(jsonPath("$[0].authorName").value("관리자"));
    }
}
