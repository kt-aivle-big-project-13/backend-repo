package com.aivle13.fin_audit_ai.domain.board.service.comment;

import com.aivle13.fin_audit_ai.domain.board.dto.response.comment.CommentResponse;
import com.aivle13.fin_audit_ai.domain.board.entity.CommentEntity;
import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;
import com.aivle13.fin_audit_ai.domain.board.repository.CommentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostRepository;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.exception.board.PostNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CommentQueryServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentQueryService commentQueryService;

    @Test
    void 게시글의_댓글을_작성_순서대로_조회한다() {
        UserEntity admin = UserEntity.create("관리자", "테스트기관", "admin@example.com", "hash", UserRole.ADMIN);
        ReflectionTestUtils.setField(admin, "id", 99L);
        PostEntity post = PostEntity.create(admin, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 10L);
        CommentEntity first = CommentEntity.create(post, admin, "첫 번째 답변");
        CommentEntity second = CommentEntity.create(post, admin, "두 번째 답변");
        given(postRepository.existsById(10L)).willReturn(true);
        given(commentRepository.findByPost_IdOrderByCreatedAtAscIdAsc(10L)).willReturn(List.of(first, second));

        List<CommentResponse> result = commentQueryService.list(10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).content()).isEqualTo("첫 번째 답변");
        assertThat(result.get(1).content()).isEqualTo("두 번째 답변");
    }

    @Test
    void 존재하지_않는_게시글의_댓글_조회시_예외() {
        given(postRepository.existsById(10L)).willReturn(false);

        assertThatThrownBy(() -> commentQueryService.list(10L))
                .isInstanceOf(PostNotFoundException.class);
    }
}
