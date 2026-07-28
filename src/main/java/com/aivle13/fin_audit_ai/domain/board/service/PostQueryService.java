package com.aivle13.fin_audit_ai.domain.board.service;

import com.aivle13.fin_audit_ai.domain.board.dto.response.PostDetailResponse;
import com.aivle13.fin_audit_ai.domain.board.dto.response.PostSummaryResponse;
import com.aivle13.fin_audit_ai.domain.board.entity.PostAttachmentEntity;
import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;
import com.aivle13.fin_audit_ai.domain.board.repository.CommentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostAttachmentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostCommentCountProjection;
import com.aivle13.fin_audit_ai.domain.board.repository.PostRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostSpecifications;
import com.aivle13.fin_audit_ai.global.dto.PageResponse;
import com.aivle13.fin_audit_ai.global.exception.board.AttachmentNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.board.PostNotFoundException;
import com.aivle13.fin_audit_ai.global.s3.dto.DownloadedFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostRepository postRepository;
    private final PostAttachmentRepository attachmentRepository;
    private final CommentRepository commentRepository;
    private final FileStorageService fileStorageService;

    public PageResponse<PostSummaryResponse> list(int page, int size, String keyword, String sort) {
        Sort.Direction direction = "oldest".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;

        // 공지(pinned) 게시글은 정렬 기준과 무관하게 항상 최상단에 노출한다.
        Sort sortOrder = Sort.by(Sort.Order.desc("pinned"), new Sort.Order(direction, "createdAt"));
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, sortOrder);

        Specification<PostEntity> spec = Specification.where(PostSpecifications.keywordContains(keyword));
        Page<PostEntity> result = postRepository.findAll(spec, pageable);

        List<Long> postIds = result.getContent().stream().map(PostEntity::getId).toList();
        Map<Long, Long> commentCounts = postIds.isEmpty()
                ? Map.of()
                : commentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        PostCommentCountProjection::getPostId,
                        PostCommentCountProjection::getCommentCount
                ));

        List<PostSummaryResponse> content = result.getContent().stream()
                .map(post -> PostSummaryResponse.of(post, commentCounts.getOrDefault(post.getId(), 0L)))
                .toList();

        return PageResponse.of(result, content);
    }

    public PostDetailResponse getDetail(Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        List<PostAttachmentEntity> attachments = attachmentRepository.findByPost_Id(postId);

        return PostDetailResponse.of(post, attachments);
    }

    public record AttachmentDownload(String filename, String contentType, long size, InputStream content) {
    }

    public AttachmentDownload downloadAttachment(Long postId, Long attachmentId) {
        PostAttachmentEntity attachment = attachmentRepository.findByIdAndPost_Id(attachmentId, postId)
                .orElseThrow(AttachmentNotFoundException::new);

        DownloadedFile file = fileStorageService.download(attachment.getFileKey());

        return new AttachmentDownload(
                attachment.getOriginalName(),
                file.contentType(),
                file.contentLength(),
                file.content()
        );
    }
}