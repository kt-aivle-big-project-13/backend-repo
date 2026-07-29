package com.aivle13.fin_audit_ai.domain.board.dto.response.post;

import com.aivle13.fin_audit_ai.domain.board.entity.PostAttachmentEntity;
import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        String authorName,
        boolean pinned,
        List<AttachmentResponse> attachments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostDetailResponse of(PostEntity post, List<PostAttachmentEntity> attachments) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getId(),
                post.getAuthor().getName(),
                post.isPinned(),
                attachments.stream().map(AttachmentResponse::from).toList(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}