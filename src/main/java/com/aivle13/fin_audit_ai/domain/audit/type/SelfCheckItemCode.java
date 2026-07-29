package com.aivle13.fin_audit_ai.domain.audit.type;

public enum SelfCheckItemCode {
    NOTICE, OBJECTION, OVERSIGHT, RISK_MANAGEMENT, DOCUMENTATION;

    // enum 값을 전부 다뤄 컴파일러가 누락을 잡아주도록 default 없이 둔다 — 새 항목을
    // 추가하고 여기 안 채우면 컴파일이 깨진다.
    public String label() {
        return switch (this) {
            case NOTICE -> "AI 심사 사실 사전 고지 여부";
            case OBJECTION -> "심사 결과 이의제기 절차 마련 여부";
            case OVERSIGHT -> "AI 결정에 대한 사람의 관리·감독 체계 여부";
            case RISK_MANAGEMENT -> "위험관리 규정 수립·운영 여부";
            case DOCUMENTATION -> "조치 내용 문서화·보관 여부";
        };
    }
}
