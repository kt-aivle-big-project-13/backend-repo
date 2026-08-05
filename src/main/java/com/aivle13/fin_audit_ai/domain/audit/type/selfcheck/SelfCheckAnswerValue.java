package com.aivle13.fin_audit_ai.domain.audit.type.selfcheck;

/**
 * 자가점검 문항 하나의 응답값. 예전엔 boolean(예/아니오)만 있었는데, 조건부 문항
 * 4개(TR-04, SC-02, SC-03, SC-05)에 "해당없음"이 추가되면서 3값으로 늘었다.
 *
 * <p>"미응답"은 별도 값을 두지 않는다 — 문항에 응답하지 않으면 SELF_CHECK_ANSWERS에
 * 그 문항의 행 자체가 없는 것으로 표현한다(안 만들어진 행 = 미응답). 그래서 '아니오'와
 * '미응답'이 저장 단계에서 섞일 일이 없다.
 */
public enum SelfCheckAnswerValue {
    YES, NO, NA;

    public String label() {
        return switch (this) {
            case YES -> "예";
            case NO -> "아니요";
            case NA -> "해당없음";
        };
    }
}
