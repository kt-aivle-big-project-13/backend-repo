package com.aivle13.fin_audit_ai.domain.user.dto.response.withdraw;

public record WithdrawResponse(
        String message
) {

    public static WithdrawResponse success() {
        return new WithdrawResponse(
                "회원 탈퇴가 완료되었습니다."
        );
    }
}