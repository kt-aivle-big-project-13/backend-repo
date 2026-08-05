package com.aivle13.fin_audit_ai.domain.board.service.post;

import com.aivle13.fin_audit_ai.domain.board.repository.CommentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostAttachmentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostRepository;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// TODO(#252): board/dashboard/diagnosis 테스트 스켈레톤. @Disabled를 지우고 본문을 채운다.
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

    @Test
    @Disabled("TODO: 구현 예정")
    void 게시글을_작성하면_저장하고_상세_응답을_반환한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 존재하지_않는_작성자면_작성시_예외() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 첨부파일이_기존과_합쳐_5개를_초과하면_예외() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 작성자_본인은_게시글을_수정할_수_있다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 작성자도_관리자도_아니면_수정시_예외() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 수정시_삭제_요청된_첨부파일은_S3에서도_함께_삭제된다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 게시글_삭제시_댓글과_첨부파일도_함께_삭제된다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 관리자는_게시글을_공지로_고정_해제할_수_있다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 관리자가_아니면_공지_고정시_예외() {
    }
}
