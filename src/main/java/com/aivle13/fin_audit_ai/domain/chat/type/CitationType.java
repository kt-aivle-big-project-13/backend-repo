package com.aivle13.fin_audit_ai.domain.chat.type;

import java.util.Arrays;
import java.util.Optional;

public enum CitationType {
    AUDIT_METRIC, GROUP_STAT, LAW_ARTICLE, REPORT_SECTION;

    /**
     * AI 서버가 보낸 문자열을 enum 으로 바꾼다.
     *
     * <p>계약에 없는 값이나 {@code null} 이 와도 예외를 던지지 않고 비어있는 값을 돌려준다.
     * 인용은 답변 신뢰의 근거라 임의로 건너뛰거나 기본값으로 저장하면 안 되고, 호출부가
     * 502 로 실패시킨다.
     */
    public static Optional<CitationType> from(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(raw.trim()))
                .findFirst();
    }
}
