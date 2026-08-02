package com.aivle13.fin_audit_ai.domain.chat.type;

import java.util.Arrays;
import java.util.Optional;

/**
 * 답변이 주입된 근거에 얼마나 기반했는지. AI 서버가 코드로 판정해 돌려준다.
 *
 * <p>{@code NOT_GROUNDED} 는 근거가 없어 답변을 거부한 경우다.
 */
public enum GroundingStatus {
    GROUNDED, PARTIAL, NOT_GROUNDED;

    /**
     * AI 서버가 보낸 문자열을 enum 으로 바꾼다.
     *
     * <p>계약에 없는 값이나 {@code null} 이 와도 예외를 던지지 않고 비어있는 값을 돌려준다.
     * 외부 서버의 계약 위반을 500 이 아니라 502 로 분류하려면 호출부가 판단해야 하기 때문이다.
     */
    public static Optional<GroundingStatus> from(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(raw.trim()))
                .findFirst();
    }
}
