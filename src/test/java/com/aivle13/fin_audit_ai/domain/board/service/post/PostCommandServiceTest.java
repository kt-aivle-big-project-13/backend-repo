package com.aivle13.fin_audit_ai.domain.board.service.post;

import com.aivle13.fin_audit_ai.domain.board.dto.request.post.PostCreateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.request.post.PostUpdateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.response.post.PostDetailResponse;
import com.aivle13.fin_audit_ai.domain.board.entity.CommentEntity;
import com.aivle13.fin_audit_ai.domain.board.entity.PostAttachmentEntity;
import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;
import com.aivle13.fin_audit_ai.domain.board.repository.CommentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostAttachmentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostRepository;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.exception.user.UserNotFoundException;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostCommandServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostAttachmentRepository attachmentRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private PostCommandService postCommandService;

    private UserEntity user(Long id, UserRole role) {
        UserEntity user = UserEntity.create("홍길동", "테스트기관", "user" + id + "@example.com", "hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void 게시글을_작성하면_저장하고_상세_응답을_반환한다() {
        UserEntity author = user(1L, UserRole.AUDITOR);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(attachmentRepository.findByPost_Id(any())).willReturn(List.of());

        PostCreateRequest request = new PostCreateRequest("제목", "내용", null);

        PostDetailResponse response = postCommandService.create(1L, request);

        ArgumentCaptor<PostEntity> captor = ArgumentCaptor.forClass(PostEntity.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("제목");
        assertThat(captor.getValue().getContent()).isEqualTo("내용");
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.authorId()).isEqualTo(1L);
    }

    @Test
    void 존재하지_않는_작성자면_작성시_예외() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());
        PostCreateRequest request = new PostCreateRequest("제목", "내용", null);

        assertThatThrownBy(() -> postCommandService.create(1L, request))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void 첨부파일이_기존과_합쳐_5개를_초과하면_예외() {
        UserEntity author = user(1L, UserRole.AUDITOR);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.findByIdForUpdate(any())).willReturn(Optional.of(PostEntity.create(author, "x", "y")));
        given(attachmentRepository.countByPost_Id(any())).willReturn(2L);

        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            MultipartFile file = mock(MultipartFile.class);
            given(file.isEmpty()).willReturn(false);
            files.add(file);
        }
        PostCreateRequest request = new PostCreateRequest("제목", "내용", files);

        assertThatThrownBy(() -> postCommandService.create(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ATTACHMENT_LIMIT_EXCEEDED));
    }

    @Test
    void 작성자_본인은_게시글을_수정할_수_있다() {
        UserEntity author = user(1L, UserRole.AUDITOR);
        PostEntity post = PostEntity.create(author, "old title", "old content");
        ReflectionTestUtils.setField(post, "id", 10L);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(attachmentRepository.findByPost_Id(10L)).willReturn(List.of());

        PostUpdateRequest request = new PostUpdateRequest("new title", "new content", null, null);

        PostDetailResponse response = postCommandService.update(1L, 10L, request);

        assertThat(response.title()).isEqualTo("new title");
        assertThat(post.getTitle()).isEqualTo("new title");
    }

    @Test
    void 작성자도_관리자도_아니면_수정시_예외() {
        UserEntity author = user(1L, UserRole.AUDITOR);
        UserEntity requester = user(2L, UserRole.AUDITOR);
        PostEntity post = PostEntity.create(author, "title", "content");
        ReflectionTestUtils.setField(post, "id", 10L);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(2L)).willReturn(Optional.of(requester));

        PostUpdateRequest request = new PostUpdateRequest("new title", "new content", null, null);

        assertThatThrownBy(() -> postCommandService.update(2L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    void 수정시_삭제_요청된_첨부파일은_S3에서도_함께_삭제된다() {
        UserEntity author = user(1L, UserRole.AUDITOR);
        PostEntity post = PostEntity.create(author, "title", "content");
        ReflectionTestUtils.setField(post, "id", 10L);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        PostAttachmentEntity attachment1 = PostAttachmentEntity.create(post, new StoredFile("key1", "a.png", "image/png", 1L));
        PostAttachmentEntity attachment2 = PostAttachmentEntity.create(post, new StoredFile("key2", "b.png", "image/png", 1L));
        given(attachmentRepository.findByPost_IdAndIdIn(10L, List.of(101L, 102L)))
                .willReturn(List.of(attachment1, attachment2));
        given(attachmentRepository.findByPost_Id(10L)).willReturn(List.of());

        PostUpdateRequest request = new PostUpdateRequest("title", "content", null, List.of(101L, 102L));

        postCommandService.update(1L, 10L, request);

        verify(attachmentRepository).deleteAll(List.of(attachment1, attachment2));
        verify(fileStorageService).deleteAfterCommit(List.of("key1", "key2"));
    }

    @Test
    void 게시글_삭제시_댓글과_첨부파일도_함께_삭제된다() {
        UserEntity author = user(1L, UserRole.AUDITOR);
        PostEntity post = PostEntity.create(author, "title", "content");
        ReflectionTestUtils.setField(post, "id", 10L);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        PostAttachmentEntity attachment = PostAttachmentEntity.create(post, new StoredFile("k1", "a.png", "image/png", 1L));
        given(attachmentRepository.findByPost_Id(10L)).willReturn(List.of(attachment));

        CommentEntity comment = CommentEntity.create(post, author, "댓글");
        given(commentRepository.findByPost_Id(10L)).willReturn(List.of(comment));

        postCommandService.delete(1L, 10L);

        verify(commentRepository).deleteAll(List.of(comment));
        verify(attachmentRepository).deleteAll(List.of(attachment));
        verify(postRepository).delete(post);
        verify(fileStorageService).deleteAfterCommit(List.of("k1"));
    }

    @Test
    void 관리자는_게시글을_공지로_고정_해제할_수_있다() {
        UserEntity admin = user(99L, UserRole.ADMIN);
        given(userRepository.findById(99L)).willReturn(Optional.of(admin));

        UserEntity author = user(1L, UserRole.AUDITOR);
        PostEntity post = PostEntity.create(author, "title", "content");
        ReflectionTestUtils.setField(post, "id", 10L);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(attachmentRepository.findByPost_Id(10L)).willReturn(List.of());

        PostDetailResponse response = postCommandService.updatePinned(99L, 10L, true);

        assertThat(response.pinned()).isTrue();
        assertThat(post.isPinned()).isTrue();
    }

    @Test
    void 관리자가_아니면_공지_고정시_예외() {
        UserEntity requester = user(1L, UserRole.AUDITOR);
        given(userRepository.findById(1L)).willReturn(Optional.of(requester));

        assertThatThrownBy(() -> postCommandService.updatePinned(1L, 10L, true))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }
}
