package com.aivle13.fin_audit_ai.domain.law.dto;

import com.aivle13.fin_audit_ai.domain.audit.type.selfcheck.SelfCheckItemCode;

// 조문 하나가 여러 자율점검 문항에 걸릴 수 있고, 문항마다 해당하는 항이 다를 수 있어(예: 제34조는
// 위험관리방안①1호·관리감독①4호·문서화①5호로 문항마다 다른 항이 걸린다) 문항과 항을 쌍으로 묶는다.
// note는 AuditRegulationMappingService.ARTICLE_MAPPING의 LawMapping.note를 그대로 노출한
// 것으로, 이 문항·항이 왜 매칭됐는지에 대한 항 단위 근거다.
public record MatchedChecklistItem(SelfCheckItemCode itemCode, String clauseNo, String note) {
}
