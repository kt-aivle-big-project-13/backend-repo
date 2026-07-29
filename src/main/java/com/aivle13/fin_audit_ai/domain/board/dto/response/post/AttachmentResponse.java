package com.aivle13.fin_audit_ai.domain.board.dto.response.post;

import com.aivle13.fin_audit_ai.domain.board.entity.PostAttachmentEntity;

public record AttachmentResponse(
        Long id,
        String originalName,
        long size
) {
    public static AttachmentResponse from(PostAttachmentEntity attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalName(),
                attachment.getSize()
        );
    }
}