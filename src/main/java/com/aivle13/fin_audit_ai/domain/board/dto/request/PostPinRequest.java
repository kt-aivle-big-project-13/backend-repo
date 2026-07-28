package com.aivle13.fin_audit_ai.domain.board.dto.request;

import jakarta.validation.constraints.NotNull;

public record PostPinRequest(
        @NotNull(message = "고정 여부를 입력해주세요.")
        Boolean pinned
) {
}