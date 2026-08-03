package com.aivle13.fin_audit_ai.domain.audit.type;

public enum FairnessStatus {
    PASS, REVIEW, FAIL;

    // 프론트 fairnessData.ts의 FAIRNESS_STATUS_LABEL과 맞춘 한글 표기.
    public String label() {
        return switch (this) {
            case PASS -> "충족";
            case REVIEW -> "주의";
            case FAIL -> "추가검토";
        };
    }
}