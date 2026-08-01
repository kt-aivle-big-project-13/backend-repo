package com.aivle13.fin_audit_ai.domain.report.type;

public enum ReportType {
    XAI_REPORT,             // 설명가능성 보고서
    BIAS_REPORT,            // 편향 진단 보고서
    HIGH_IMPACT_REPORT,        // 고영향 AI 사전진단 보고서
    COMPLIANCE_VERDICT,     // 규제 준수 판정서
    IMPROVEMENT_GUIDE,      // 개선 권고 가이드
    FINAL_AUDIT_REPORT      // 최종 보고서 = 설명 가능성 + 편향진단 + 규제준수 + 개선 권고 가이드
}
