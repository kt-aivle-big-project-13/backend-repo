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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
        createPost(user, "첫 번째 공지", "내용1");
        createPost(user, "두 번째 글", "내용2");

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
        PostEntity post = createPost(user, "상세 조회 테스트", "본문 내용");

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
    @DisplayName("게시글을 작성하면 201과 함께 게시글이 생성된다")
    void create_success() throws Exception {
        mockMvc.perform(multipart("/api/v1/posts")
                        .param("title", "새 게시글")
                        .param("content", "새 게시글 내용")
                        .with(authentication(asUser(user))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("새 게시글"))
                .andExpect(jsonPath("$.authorId").value(user.getId()));
    }

    @Test
    @DisplayName("첨부파일이 최대 개수를 초과하면 400을 반환한다")
    void create_attachmentLimitExceeded() throws Exception {
        when(fileStorageService.store(any(), anyString()))
                .thenReturn(new StoredFile("board-posts/key", "file.png", "image/png", 100L));

        var multipartRequest = multipart("/api/v1/posts")
                .param("title", "첨부 초과 테스트")
                .param("content", "내용");
        for (int i = 0; i < 6; i++) {
            multipartRequest.file(new MockMultipartFile("files", "file" + i + ".png", "image/png", new byte[]{1}));
        }

        mockMvc.perform(multipartRequest.with(authentication(asUser(user))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("작성자 본인이 게시글을 수정한다")
    void update_byAuthor_success() throws Exception {
        PostEntity post = createPost(user, "수정 전 제목", "수정 전 내용");

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/posts/{postId}", post.getId())
                        .param("title", "수정 후 제목")
                        .param("content", "수정 후 내용")
                        .with(authentication(asUser(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정 후 제목"));
    }

    @Test
    @DisplayName("작성자가 아니고 관리자도 아니면 수정 시 403을 반환한다")
    void update_forbidden() throws Exception {
        PostEntity post = createPost(user, "제목", "내용");

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/posts/{postId}", post.getId())
                        .param("title", "다른 사람이 수정")
                        .param("content", "다른 사람이 수정")
                        .with(authentication(asUser(otherUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("작성자 본인이 게시글을 삭제하면 204를 반환하고 게시글이 사라진다")
    void delete_byAuthor_success() throws Exception {
        PostEntity post = createPost(user, "삭제될 글", "내용");

        mockMvc.perform(delete("/api/v1/posts/{postId}", post.getId())
                        .with(authentication(asUser(user))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/posts/{postId}", post.getId())
                        .with(authentication(asUser(user))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("관리자가 게시글을 공지로 고정한다")
    void pin_byAdmin_success() throws Exception {
        PostEntity post = createPost(user, "공지 대상", "내용");

        mockMvc.perform(patch("/api/v1/posts/{postId}/pin", post.getId())
                        .contentType("application/json")
                        .content("{\"pinned\": true}")
                        .with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(true));
    }

    @Test
    @DisplayName("관리자가 아니면 공지 고정 시 403을 반환한다")
    void pin_forbidden() throws Exception {
        PostEntity post = createPost(user, "공지 대상", "내용");

        mockMvc.perform(patch("/api/v1/posts/{postId}/pin", post.getId())
                        .contentType("application/json")
                        .content("{\"pinned\": true}")
                        .with(authentication(asUser(user))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("첨부파일을 다운로드한다")
    void download_success() throws Exception {
        PostEntity post = createPost(user, "첨부파일 있는 글", "내용");
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
