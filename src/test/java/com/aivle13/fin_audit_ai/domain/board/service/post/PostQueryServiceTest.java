package com.aivle13.fin_audit_ai.domain.board.service.post;

import com.aivle13.fin_audit_ai.domain.board.dto.response.post.PostDetailResponse;
import com.aivle13.fin_audit_ai.domain.board.dto.response.post.PostSummaryResponse;
import com.aivle13.fin_audit_ai.domain.board.entity.PostAttachmentEntity;
import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;
import com.aivle13.fin_audit_ai.domain.board.repository.PostAttachmentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostRepository;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.dto.PageResponse;
import com.aivle13.fin_audit_ai.global.exception.board.AttachmentNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.board.PostNotFoundException;
import com.aivle13.fin_audit_ai.global.s3.dto.DownloadedFile;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostAttachmentRepository attachmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private PostQueryService postQueryService;

    private PostEntity post(Long id, String title) {
        UserEntity author = UserEntity.create("홍길동", "테스트기관", "author@example.com", "hash", UserRole.AUDITOR);
        ReflectionTestUtils.setField(author, "id", 1L);
        PostEntity post = PostEntity.create(author, title, "내용");
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    // 키워드/정렬 조건 자체는 PostSpecifications가 JPA Criteria로 구성하므로
    // 순수 단위 테스트로는 검증할 수 없다(실제 쿼리 실행이 필요). 여기서는 서비스가
    // 리포지토리에 올바른 Pageable로 위임하고 결과를 응답으로 조립하는 부분만 검증한다.
    @Test
    void 첫_페이지_요청은_0번_인덱스_Pageable로_변환해_조회한다() {
        given(postRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        postQueryService.list(1, 10, null, "latest");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void 조회된_공지사항을_요약_응답으로_반환한다() {
        PostEntity post1 = post(1L, "첫 글");
        PostEntity post2 = post(2L, "댓글 없는 글");
        Page<PostEntity> page = new PageImpl<>(List.of(post1, post2), PageRequest.of(0, 10), 2);
        given(postRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(page);
        PageResponse<PostSummaryResponse> result = postQueryService.list(1, 10, null, "latest");

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).title()).isEqualTo("첫 글");
    }

    @Test
    void 조회_결과가_없으면_빈_목록을_반환한다() {
        given(postRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        PageResponse<PostSummaryResponse> result = postQueryService.list(1, 10, "검색어", "oldest");

        assertThat(result.content()).isEmpty();
    }

    @Test
    void 게시글_상세를_첨부파일_목록과_함께_조회한다() {
        PostEntity post = post(1L, "상세 조회 대상");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        PostAttachmentEntity attachment = PostAttachmentEntity.create(
                post, new StoredFile("key", "a.png", "image/png", 10L));
        given(attachmentRepository.findByPost_Id(1L)).willReturn(List.of(attachment));

        PostDetailResponse response = postQueryService.getDetail(1L);

        assertThat(response.title()).isEqualTo("상세 조회 대상");
        assertThat(response.attachments()).hasSize(1);
        assertThat(response.attachments().get(0).originalName()).isEqualTo("a.png");
    }

    @Test
    void 존재하지_않는_게시글_조회시_예외() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postQueryService.getDetail(999L))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void 첨부파일을_다운로드하면_S3에서_스트림을_가져온다() {
        PostEntity post = post(1L, "첨부 있는 글");
        PostAttachmentEntity attachment = PostAttachmentEntity.create(
                post, new StoredFile("board-posts/key", "report.pdf", "application/pdf", 4L));
        ReflectionTestUtils.setField(attachment, "id", 5L);
        given(attachmentRepository.findByIdAndPost_Id(5L, 1L)).willReturn(Optional.of(attachment));
        given(fileStorageService.download("board-posts/key"))
                .willReturn(new DownloadedFile(new ByteArrayInputStream(new byte[]{1, 2}), "application/pdf", 2L));

        PostQueryService.AttachmentDownload download = postQueryService.downloadAttachment(1L, 5L);

        assertThat(download.filename()).isEqualTo("report.pdf");
        assertThat(download.contentType()).isEqualTo("application/pdf");
        assertThat(download.size()).isEqualTo(2L);
    }

    @Test
    void 존재하지_않는_첨부파일_다운로드시_예외() {
        given(attachmentRepository.findByIdAndPost_Id(5L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postQueryService.downloadAttachment(1L, 5L))
                .isInstanceOf(AttachmentNotFoundException.class);
    }
}
