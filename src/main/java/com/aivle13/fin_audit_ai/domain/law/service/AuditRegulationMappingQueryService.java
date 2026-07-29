package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.law.dto.AuditRegulationComplianceView;

import java.util.List;

public interface AuditRegulationMappingQueryService {

    // 감사에 매칭된 법령 조항별 준수 판정 조회
    List<AuditRegulationComplianceView> getMappings(Long auditId);
}
