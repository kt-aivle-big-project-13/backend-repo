package com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;

import java.math.BigDecimal;

/**
 * 감사셋 전체 모델 판별 성능 응답. 값이 없으면(성능 미산출 감사) null 필드로 남는다.
 */
public record PerformanceResponse(
        BigDecimal auc,
        BigDecimal accuracy
) {

    // 성능 값이 하나도 없으면(구버전 감사 등) 응답에서 통째로 생략하도록 null 을 돌려준다.
    public static PerformanceResponse from(AuditEntity audit) {
        if (audit == null || (audit.getModelAuc() == null && audit.getModelAccuracy() == null)) {
            return null;
        }
        return new PerformanceResponse(audit.getModelAuc(), audit.getModelAccuracy());
    }
}
