package com.aivle13.fin_audit_ai.domain.board.service.post;

import com.aivle13.fin_audit_ai.domain.board.repository.CommentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostAttachmentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostRepository;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// TODO(#252): board/dashboard/diagnosis 테스트 스켈레톤. @Disabled를 지우고 본문을 채운다.
@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostAttachmentRepository attachmentRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private PostQueryService postQueryService;

    @Test
    @Disabled("TODO: 구현 예정")
    void 키워드로_제목_내용을_검색해_목록을_조회한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 공지_게시글은_정렬_옵션과_무관하게_최상단에_노출된다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void sort가_oldest면_오래된_순으로_정렬한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 게시글_상세를_첨부파일_목록과_함께_조회한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 존재하지_않는_게시글_조회시_예외() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 첨부파일을_다운로드하면_S3에서_스트림을_가져온다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 존재하지_않는_첨부파일_다운로드시_예외() {
    }
}
