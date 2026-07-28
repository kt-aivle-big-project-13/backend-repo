// 보고서 프롬포트 빌더 작업으로 인해 임시로 만든 것

package com.aivle13.fin_audit_ai.domain.law.dto;

import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;

// 보고서 생성에 사용할 확정 법령 매핑 조회 결과
public record AuditLawComplianceView(
        Long mappingId,
        String articleNumber,
        String articleTitle,
        ComplianceStatus compliance,
        String evidence
) {
}