package com.aivle13.fin_audit_ai.domain.audit.dto.request;

import java.math.BigDecimal;

/**
 * AI 서버 {@code POST /api/fairness/audits} 호출에 필요한 정보.
 * 실제 멀티파트 요청 조립(S3 파일 스트림 읽기 등)은 클라이언트 구현체가 담당한다.
 */
public record FairnessRunRequest(
        Long auditId,
        String modelFileKey,
        String auditDatasetFileKey,

        // 없으면 AI 서버가 감사 데이터셋으로 폴백
        String validationDatasetFileKey,

        String auditName,

        // 둘 다 optional. 수동값이 있으면 우선, 없으면 목표 승인율로 산출, 둘 다 없으면 AI 서버 기본값(0.90) 사용
        BigDecimal targetApprovalRate,
        BigDecimal manualThreshold,

        // 콤마 구분 보호속성 컬럼명 (예: "CODE_GENDER,AGE_GROUP")
        String sensitiveFeatures
) {
}
