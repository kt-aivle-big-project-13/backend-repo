package com.aivle13.fin_audit_ai.domain.user.dto.request.notification;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferencesRequest(

        @NotNull(message = "법령 개정 이메일 알림 설정값을 입력해주세요.")
        Boolean lawEmailEnabled,

        @NotNull(message = "재감사 권고 알림 설정값을 입력해주세요.")
        Boolean reauditAlertEnabled,

        @NotNull(message = "감사 완료 알림 설정값을 입력해주세요.")
        Boolean auditCompleteAlertEnabled

) {
}