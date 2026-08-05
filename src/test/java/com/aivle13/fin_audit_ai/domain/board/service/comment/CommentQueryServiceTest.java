package com.aivle13.fin_audit_ai.domain.board.service.comment;

import com.aivle13.fin_audit_ai.domain.board.repository.CommentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// TODO(#252): board/dashboard/diagnosis 테스트 스켈레톤. @Disabled를 지우고 본문을 채운다.
@ExtendWith(MockitoExtension.class)
class CommentQueryServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentQueryService commentQueryService;

    @Test
    @Disabled("TODO: 구현 예정")
    void 게시글의_댓글을_작성_순서대로_조회한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 존재하지_않는_게시글의_댓글_조회시_예외() {
    }
}
