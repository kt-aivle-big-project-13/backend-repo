package com.aivle13.fin_audit_ai.domain.board.repository;

public interface PostCommentCountProjection {
    Long getPostId();
    Long getCommentCount();
}