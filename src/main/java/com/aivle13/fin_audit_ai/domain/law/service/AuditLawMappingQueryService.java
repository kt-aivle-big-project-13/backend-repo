package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.law.dto.AuditLawComplianceView;

import java.util.List;

public interface AuditLawMappingQueryService {

    // 담당자가 검토를 완료한 법령 매핑 조회
    List<AuditLawComplianceView> getConfirmedMappings(Long auditId);

    // 검토되지 않은 PENDING 매핑 존재 여부
    boolean hasPendingMappings(Long auditId);
}