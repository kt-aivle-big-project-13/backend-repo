package com.aivle13.fin_audit_ai.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageCreateRequest(

        @NotBlank(message = "질문을 입력해주세요.")
        @Size(max = 2000, message = "질문은 2000자를 넘을 수 없습니다.")
        String question
) {
}
