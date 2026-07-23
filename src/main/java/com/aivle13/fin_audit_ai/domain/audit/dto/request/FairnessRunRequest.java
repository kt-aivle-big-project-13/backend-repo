package com.aivle13.fin_audit_ai.domain.audit.dto.request;

import java.math.BigDecimal;

/**
 * AI 서버 {@code POST /internal/v1/fairness/analyze} 호출에 필요한 정보.
 * 파일을 직접 전달하지 않고, 모델·데이터셋의 S3 객체 키를 JSON으로 전송한다.
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
