package com.aivle13.fin_audit_ai.domain.audit.type;

public enum FairnessMetricCode {
    DEMOGRAPHIC_PARITY, EQUALIZED_ODDS, EQUAL_OPPORTUNITY, PROPORTIONAL_PARITY,
    FPR_PARITY, FDR_PARITY, FOR_PARITY;

    // 프론트 fairnessData.ts의 COLUMN_TOOLTIP 제목과 맞춘 한글 표기 — 챗봇 답변에 이 값이
    // 그대로 노출되지 않도록 한다. 새 값을 추가하면 여기도 채워야 하므로 default를 두지 않는다.
    public String label() {
        return switch (this) {
            case PROPORTIONAL_PARITY -> "비례성 패리티(Proportional Parity)";
            case DEMOGRAPHIC_PARITY -> "인구통계학적 평등성(Demographic Parity)";
            case EQUAL_OPPORTUNITY -> "기회의 균등(Equal Opportunity)";
            case EQUALIZED_ODDS -> "균등화된 오즈(Equalized Odds)";
            case FPR_PARITY -> "거짓 양성률 패리티(FPR Parity)";
            case FDR_PARITY -> "거짓 발견율 패리티(FDR Parity)";
            case FOR_PARITY -> "거짓 누락률 패리티(FOR Parity)";
        };
    }
}