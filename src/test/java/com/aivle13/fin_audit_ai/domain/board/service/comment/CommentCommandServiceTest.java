package com.aivle13.fin_audit_ai.domain.board.service.comment;

import com.aivle13.fin_audit_ai.domain.board.dto.request.comment.CommentCreateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.request.comment.CommentUpdateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.response.comment.CommentResponse;
import com.aivle13.fin_audit_ai.domain.board.entity.CommentEntity;
import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;
import com.aivle13.fin_audit_ai.domain.board.repository.CommentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostRepository;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.exception.board.CommentNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.board.PostNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.user.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentCommandServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentCommandService commentCommandService;

    private UserEntity user(Long id, String name, UserRole role) {
        UserEntity user = UserEntity.create(name, "테스트기관", "user" + id + "@example.com", "hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private PostEntity post(Long id, UserEntity author) {
        PostEntity post = PostEntity.create(author, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    @Test
    void 댓글을_작성하면_저장하고_응답을_반환한다() {
        UserEntity author = user(1L, "일반사용자", UserRole.AUDITOR);
        PostEntity post = post(10L, author);
        UserEntity admin = user(99L, "관리자", UserRole.ADMIN);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(99L)).willReturn(Optional.of(admin));

        CommentCreateRequest request = new CommentCreateRequest("관리자 답변입니다");

        CommentResponse response = commentCommandService.create(99L, 10L, request);

        ArgumentCaptor<CommentEntity> captor = ArgumentCaptor.forClass(CommentEntity.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("관리자 답변입니다");
        assertThat(captor.getValue().getPost()).isEqualTo(post);
        assertThat(response.content()).isEqualTo("관리자 답변입니다");
        assertThat(response.authorName()).isEqualTo("관리자");
    }

    @Test
    void 존재하지_않는_게시글에_댓글_작성시_예외() {
        given(postRepository.findById(10L)).willReturn(Optional.empty());
        CommentCreateRequest request = new CommentCreateRequest("답변");

        assertThatThrownBy(() -> commentCommandService.create(99L, 10L, request))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void 존재하지_않는_작성자로_댓글_작성시_예외() {
        UserEntity author = user(1L, "일반사용자", UserRole.AUDITOR);
        PostEntity post = post(10L, author);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(99L)).willReturn(Optional.empty());
        CommentCreateRequest request = new CommentCreateRequest("답변");

        assertThatThrownBy(() -> commentCommandService.create(99L, 10L, request))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void 댓글_내용을_수정한다() {
        UserEntity admin = user(99L, "관리자", UserRole.ADMIN);
        PostEntity post = post(10L, admin);
        CommentEntity comment = CommentEntity.create(post, admin, "수정 전 내용");
        ReflectionTestUtils.setField(comment, "id", 5L);
        given(commentRepository.findByIdAndPost_Id(5L, 10L)).willReturn(Optional.of(comment));

        CommentUpdateRequest request = new CommentUpdateRequest("수정 후 내용");

        CommentResponse response = commentCommandService.update(10L, 5L, request);

        assertThat(response.content()).isEqualTo("수정 후 내용");
        assertThat(comment.getContent()).isEqualTo("수정 후 내용");
    }

    @Test
    void 존재하지_않는_댓글_수정시_예외() {
        given(commentRepository.findByIdAndPost_Id(5L, 10L)).willReturn(Optional.empty());
        CommentUpdateRequest request = new CommentUpdateRequest("수정 시도");

        assertThatThrownBy(() -> commentCommandService.update(10L, 5L, request))
                .isInstanceOf(CommentNotFoundException.class);
    }

    @Test
    void 댓글을_삭제한다() {
        UserEntity admin = user(99L, "관리자", UserRole.ADMIN);
        PostEntity post = post(10L, admin);
        CommentEntity comment = CommentEntity.create(post, admin, "삭제될 댓글");
        ReflectionTestUtils.setField(comment, "id", 5L);
        given(commentRepository.findByIdAndPost_Id(5L, 10L)).willReturn(Optional.of(comment));

        commentCommandService.delete(10L, 5L);

        verify(commentRepository).delete(comment);
    }

    @Test
    void 존재하지_않는_댓글_삭제시_예외() {
        given(commentRepository.findByIdAndPost_Id(5L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentCommandService.delete(10L, 5L))
                .isInstanceOf(CommentNotFoundException.class);
    }
}
