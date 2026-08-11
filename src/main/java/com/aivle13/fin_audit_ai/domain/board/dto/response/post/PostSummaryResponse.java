package com.aivle13.fin_audit_ai.domain.board.dto.response.post;

import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long id,
        String title,
        String authorName,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse from(PostEntity post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getAuthor().getName(),
                post.getCreatedAt()
        );
    }
}
