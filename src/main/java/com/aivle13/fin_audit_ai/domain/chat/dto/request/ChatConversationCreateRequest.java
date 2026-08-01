package com.aivle13.fin_audit_ai.domain.chat.dto.request;

import jakarta.validation.constraints.Size;

public record ChatConversationCreateRequest(

        // 비우면 기본 제목을 붙인다.
        @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
        String title
) {
}
