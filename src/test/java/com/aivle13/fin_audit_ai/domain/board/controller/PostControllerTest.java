package com.aivle13.fin_audit_ai.domain.board.controller;

import com.aivle13.fin_audit_ai.domain.board.entity.PostAttachmentEntity;
import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;
import com.aivle13.fin_audit_ai.domain.board.repository.PostAttachmentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostRepository;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.s3.dto.DownloadedFile;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class PostControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostAttachmentRepository attachmentRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    private UserEntity user;
    private UserEntity otherUser;
    private UserEntity admin;

    @BeforeEach
    void setUp() {
        user = userRepository.save(
                UserEntity.create("홍길동", "테스트기관", "post-test@example.com", "hash", UserRole.AUDITOR));
        otherUser = userRepository.save(
                UserEntity.create("김철수", "테스트기관", "post-other@example.com", "hash", UserRole.AUDITOR));
        admin = userRepository.save(
                UserEntity.create("관리자", "테스트기관", "post-admin@example.com", "hash", UserRole.ADMIN));
    }

    private Authentication asUser(UserEntity target) {
        return new UsernamePasswordAuthenticationToken(target.getId(), null, List.of());
    }

    private Authentication asAdmin() {
        return new UsernamePasswordAuthenticationToken(
                admin.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private PostEntity createPost(UserEntity author, String title, String content) {
        return postRepository.save(PostEntity.create(author, title, content));
    }

    @Test
    @DisplayName("게시글 목록을 페이지네이션·검색·정렬 조건으로 조회한다")
    void list_success() throws Exception {
        createPost(admin, "첫 번째 공지", "내용1");
        createPost(admin, "두 번째 공지", "내용2");

        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "1")
                        .param("size", "10")
                        .with(authentication(asUser(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("인증 정보가 없으면 목록 조회 시 401을 반환한다")
    void list_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("size가 최대값을 초과하면 400을 반환한다")
    void list_invalidPaging() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .param("size", "101")
                        .with(authentication(asUser(user))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("게시글 상세를 첨부파일 목록과 함께 조회한다")
    void get_success() throws Exception {
        PostEntity post = createPost(admin, "상세 조회 테스트", "본문 내용");

        mockMvc.perform(get("/api/v1/posts/{postId}", post.getId())
                        .with(authentication(asUser(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("상세 조회 테스트"))
                .andExpect(jsonPath("$.attachments").isArray());
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 조회하면 404를 반환한다")
    void get_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{postId}", 999_999L)
                        .with(authentication(asUser(user))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("관리자가 공지사항을 작성하면 201과 함께 생성된다")
    void create_success() throws Exception {
        mockMvc.perform(multipart("/api/v1/posts")
                        .param("title", "새 게시글")
                        .param("content", "새 게시글 내용")
                        .with(authentication(asAdmin())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("새 게시글"))
                .andExpect(jsonPath("$.authorId").value(admin.getId()))
                .andExpect(jsonPath("$.pinned").doesNotExist());
    }

    @Test
    @DisplayName("일반 사용자가 공지사항을 작성하면 403을 반환한다")
    void create_forbidden() throws Exception {
        mockMvc.perform(multipart("/api/v1/posts")
                        .param("title", "작성 불가")
                        .param("content", "내용")
                        .with(authentication(asUser(user))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("첨부파일이 최대 개수를 초과하면 400을 반환한다")
    void create_attachmentLimitExceeded() throws Exception {
        // 개수 초과 검증이 첨부 저장(S3 업로드)보다 먼저 일어나므로 fileStorageService.store는 호출되지 않는다.
        var multipartRequest = multipart("/api/v1/posts")
                .param("title", "첨부 초과 테스트")
                .param("content", "내용");
        for (int i = 0; i < 6; i++) {
            multipartRequest.file(new MockMultipartFile("files", "file" + i + ".png", "image/png", new byte[]{1}));
        }

        mockMvc.perform(multipartRequest.with(authentication(asAdmin())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("관리자가 공지사항을 수정한다")
    void update_byAdmin_success() throws Exception {
        PostEntity post = createPost(admin, "수정 전 제목", "수정 전 내용");

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/posts/{postId}", post.getId())
                        .param("title", "수정 후 제목")
                        .param("content", "수정 후 내용")
                        .with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정 후 제목"));
    }

    @Test
    @DisplayName("일반 사용자가 공지사항을 수정하면 403을 반환한다")
    void update_forbidden() throws Exception {
        PostEntity post = createPost(user, "제목", "내용");

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/posts/{postId}", post.getId())
                        .param("title", "다른 사람이 수정")
                        .param("content", "다른 사람이 수정")
                        .with(authentication(asUser(otherUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자가 공지사항을 삭제하면 204를 반환하고 공지사항이 사라진다")
    void delete_byAdmin_success() throws Exception {
        PostEntity post = createPost(admin, "삭제될 공지", "내용");

        mockMvc.perform(delete("/api/v1/posts/{postId}", post.getId())
                        .with(authentication(asAdmin())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/posts/{postId}", post.getId())
                        .with(authentication(asUser(user))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("첨부파일을 다운로드한다")
    void download_success() throws Exception {
        PostEntity post = createPost(admin, "첨부파일 있는 공지", "내용");
        PostAttachmentEntity attachment = attachmentRepository.save(PostAttachmentEntity.create(
                post, new StoredFile("board-posts/key", "report.pdf", "application/pdf", 4L)));

        when(fileStorageService.download("board-posts/key"))
                .thenReturn(new DownloadedFile(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}), "application/pdf", 4L));

        mockMvc.perform(get("/api/v1/posts/{postId}/attachments/{attachmentId}", post.getId(), attachment.getId())
                        .with(authentication(asUser(user))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("report.pdf")))
                .andExpect(content().bytes(new byte[]{1, 2, 3, 4}));
    }
}
