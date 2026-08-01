package com.aivle13.fin_audit_ai.domain.chat.type;

/**
 * 답변이 주입된 근거에 얼마나 기반했는지. AI 서버가 코드로 판정해 돌려준다.
 *
 * <p>{@code NOT_GROUNDED} 는 근거가 없어 답변을 거부한 경우다.
 */
public enum GroundingStatus {
    GROUNDED, PARTIAL, NOT_GROUNDED
}
