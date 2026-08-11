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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 관리자 공지사항 작성 → 일반 사용자 조회 흐름 통합 테스트.
// TestContainers(Postgres+Redis)로 실제 DB에 저장/조회되는 것까지 검증한다 (컨트롤러 단위 목킹이 아님).
// 흐름: 1) 일반 사용자 작성 차단 → 2) 관리자 작성 → 3) 일반 사용자 상세·목록 조회.
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
    @DisplayName("일반 사용자의 작성은 차단하고 관리자가 작성한 공지사항은 모두 조회할 수 있다")
    void adminCreatesNotice_userReadsNotice_flow() throws Exception {
        mockMvc.perform(multipart("/api/v1/posts")
                        .param("title", "작성 불가")
                        .param("content", "일반 사용자 본문")
                        .with(authentication(asUser())))
                .andExpect(status().isForbidden());

        MvcResult createResult = mockMvc.perform(multipart("/api/v1/posts")
                        .param("title", "통합 테스트 공지사항")
                        .param("content", "통합 테스트 본문")
                        .with(authentication(asAdmin())))
                .andExpect(status().isCreated())
                .andReturn();
        Long postId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.id", Long.class);

        mockMvc.perform(get("/api/v1/posts/{postId}", postId)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("통합 테스트 공지사항"))
                .andExpect(jsonPath("$.content").value("통합 테스트 본문"))
                .andExpect(jsonPath("$.attachments").isEmpty())
                .andExpect(jsonPath("$.pinned").doesNotExist());

        // 게시글 목록에도 반영됐는지 확인한다.
        mockMvc.perform(get("/api/v1/posts")
                .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(postId))
                .andExpect(jsonPath("$.content[0].commentCount").doesNotExist());
    }
}
