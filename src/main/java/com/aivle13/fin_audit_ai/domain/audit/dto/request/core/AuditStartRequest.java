package com.aivle13.fin_audit_ai.domain.audit.dto.request.core;

import com.aivle13.fin_audit_ai.domain.audit.type.ThresholdMethod;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AuditStartRequest(
        @NotNull Long modelId,
        @NotNull Long datasetId,
        Long assessmentId,
        @NotBlank String auditName,
        @NotNull ThresholdMethod thresholdMethod,

        // thresholdMethod=VALIDATION_DATASET일 때만 사용. AuditEntity.targetApprovalRate 컬럼(precision=5, scale=4)과 정밀도를 맞춘다.
        @DecimalMin(value = "0", inclusive = false) @DecimalMax(value = "1", inclusive = false)
        @Digits(integer = 1, fraction = 4)
        BigDecimal targetApprovalRate,

        // thresholdMethod=MANUAL일 때만 사용. AuditEntity.manualThreshold 컬럼(precision=5, scale=4)과 정밀도를 맞춘다.
        // 0은 아무도 승인되지 않는 값이라 AI 서버가 거부하므로 여기서 먼저 막는다.
        @DecimalMin(value = "0", inclusive = false) @DecimalMax("1")
        @Digits(integer = 1, fraction = 4)
        BigDecimal manualThreshold,

        // thresholdMethod=VALIDATION_DATASET일 때만 사용. 미전달 시 같은 모델 계열의 최신 VALIDATION
        // 데이터셋을 자동 선택하고, 지정하면 그 데이터셋을 검증한 뒤 사용한다.
        Long validationDatasetId
) {

    // 임계값 방식과 실제로 넘어온 값이 어긋나면 감사가 사용자가 고른 방식과 다른 기준으로
    // 수행된다. AI 서버는 수동 임계값이 있으면 그것을 우선하므로, VALIDATION_DATASET인데
    // manualThreshold가 섞여 들어오면 목표 승인율이 아니라 수동값으로 계산된다.
    @AssertTrue(message = "임계값 직접 입력 방식은 manualThreshold가 필요하고, 검증 데이터셋 방식은 manualThreshold를 받지 않습니다.")
    public boolean isManualThresholdConsistent() {
        // thresholdMethod 자체가 없으면 @NotNull이 따로 보고한다.
        if (thresholdMethod == null) {
            return true;
        }

        return thresholdMethod == ThresholdMethod.MANUAL
                ? manualThreshold != null
                : manualThreshold == null;
    }

    // 목표 승인율은 검증 데이터셋으로 임계값을 산출할 때만 의미가 있다. 미전달 시
    // AI 서버가 기본 목표 승인율로 산출하므로 VALIDATION_DATASET에서도 필수는 아니다.
    @AssertTrue(message = "목표 승인율은 검증 데이터셋 방식에서만 사용합니다.")
    public boolean isTargetApprovalRateConsistent() {
        if (thresholdMethod == null) {
            return true;
        }

        return thresholdMethod != ThresholdMethod.MANUAL
                || targetApprovalRate == null;
    }
}
