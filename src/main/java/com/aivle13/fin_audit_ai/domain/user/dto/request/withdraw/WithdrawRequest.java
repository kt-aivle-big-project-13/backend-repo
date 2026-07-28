package com.aivle13.fin_audit_ai.domain.user.dto.request.withdraw;

import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password

) {
}
