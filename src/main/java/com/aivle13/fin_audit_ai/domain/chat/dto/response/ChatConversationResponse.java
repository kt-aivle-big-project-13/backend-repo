package com.aivle13.fin_audit_ai.domain.chat.dto.response;

import com.aivle13.fin_audit_ai.domain.chat.entity.ChatConversationEntity;

import java.time.LocalDateTime;

public record ChatConversationResponse(
        Long conversationId,
        Long auditId,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ChatConversationResponse from(
            ChatConversationEntity conversation
    ) {
        return new ChatConversationResponse(
                conversation.getId(),
                conversation.getAudit().getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
