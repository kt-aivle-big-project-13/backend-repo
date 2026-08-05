package com.aivle13.fin_audit_ai.domain.audit.type.explainability;

public enum XaiStatus {
    PASS,
    WARNING,
    REVIEW;

    // 프론트 shapData.ts의 SHAP_STATUS_LABEL과 맞춘 한글 표기.
    public String label() {
        return switch (this) {
            case PASS -> "충족";
            case WARNING -> "주의";
            case REVIEW -> "추가검토";
        };
    }
}