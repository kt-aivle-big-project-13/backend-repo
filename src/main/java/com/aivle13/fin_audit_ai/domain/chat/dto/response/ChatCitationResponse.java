package com.aivle13.fin_audit_ai.domain.chat.dto.response;

import com.aivle13.fin_audit_ai.domain.chat.entity.ChatMessageCitationEntity;
import com.aivle13.fin_audit_ai.domain.chat.type.CitationType;

import java.math.BigDecimal;

public record ChatCitationResponse(
        Long citationId,
        CitationType type,
        String reference,
        String snippet,
        BigDecimal similarity,
        String sourceUrl
) {

    public static ChatCitationResponse from(ChatMessageCitationEntity citation) {
        return new ChatCitationResponse(
                citation.getId(),
                citation.getCitationType(),
                citation.getReference(),
                citation.getSnippet(),
                citation.getSimilarity(),
                citation.getSourceUrl()
        );
    }
}
