package com.aivle13.fin_audit_ai.domain.audit.type.explainability;

public enum XaiMetricCode {
    SENSITIVE_CONTRIB,
    GLOBAL_STABILITY,
    FIDELITY;

    // 프론트 shapData.ts의 라벨과 맞춘 한글 표기.
    public String label() {
        return switch (this) {
            case SENSITIVE_CONTRIB -> "민감변수 기여비율";
            case GLOBAL_STABILITY -> "설명 일관성";
            case FIDELITY -> "설명 충실성";
        };
    }
}