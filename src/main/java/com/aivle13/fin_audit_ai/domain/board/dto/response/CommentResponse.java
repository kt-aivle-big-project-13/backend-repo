package com.aivle13.fin_audit_ai.domain.board.dto.response;

import com.aivle13.fin_audit_ai.domain.board.entity.CommentEntity;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String content,
        String authorName,
        LocalDateTime createdAt
) {
    public static CommentResponse from(CommentEntity comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getAuthor().getName(),
                comment.getCreatedAt()
        );
    }
}