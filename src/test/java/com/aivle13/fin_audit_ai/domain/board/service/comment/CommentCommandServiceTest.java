package com.aivle13.fin_audit_ai.domain.board.service.comment;

import com.aivle13.fin_audit_ai.domain.board.repository.CommentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostRepository;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// TODO(#252): board/dashboard/diagnosis 테스트 스켈레톤. @Disabled를 지우고 본문을 채운다.
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

    @Test
    @Disabled("TODO: 구현 예정")
    void 댓글을_작성하면_저장하고_응답을_반환한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 존재하지_않는_게시글에_댓글_작성시_예외() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 존재하지_않는_작성자로_댓글_작성시_예외() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 댓글_내용을_수정한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 존재하지_않는_댓글_수정시_예외() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 댓글을_삭제한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 존재하지_않는_댓글_삭제시_예외() {
    }
}
