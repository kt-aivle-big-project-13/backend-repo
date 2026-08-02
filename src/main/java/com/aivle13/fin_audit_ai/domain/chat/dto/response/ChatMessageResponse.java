package com.aivle13.fin_audit_ai.domain.chat.dto.response;

import com.aivle13.fin_audit_ai.domain.chat.entity.ChatMessageEntity;
import com.aivle13.fin_audit_ai.domain.chat.type.ChatRole;
import com.aivle13.fin_audit_ai.domain.chat.type.GroundingStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageResponse(
        Long messageId,
        ChatRole role,
        String content,
        GroundingStatus groundingStatus,
        List<ChatCitationResponse> citations,
        LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessageEntity message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getGroundingStatus(),
                message.getCitations().stream()
                        .map(ChatCitationResponse::from)
                        .toList(),
                message.getCreatedAt()
        );
    }
}
