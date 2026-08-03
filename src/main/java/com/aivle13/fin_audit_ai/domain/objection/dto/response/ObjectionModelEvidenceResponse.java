package com.aivle13.fin_audit_ai.domain.objection.dto.response;

import com.aivle13.fin_audit_ai.domain.audit.entity.ShapFeatureImportanceEntity;

import java.math.BigDecimal;

public record ObjectionModelEvidenceResponse(
        Integer rank,
        String feature,
        String displayName,
        BigDecimal meanAbsShap,
        BigDecimal contributionRatio,
        String direction
) {

    public static ObjectionModelEvidenceResponse from(
            ShapFeatureImportanceEntity entity
    ) {
        return new ObjectionModelEvidenceResponse(
                entity.getRank(),
                entity.getFeature(),
                toDisplayName(entity.getFeature()),
                entity.getMeanAbsShap(),
                entity.getContributionRatio(),
                entity.getDirection()
        );
    }

    private static String toDisplayName(String feature) {
        return switch (feature) {
            case "EXT_SOURCE_1" -> "외부 평가 지표 1";
            case "EXT_SOURCE_2" -> "외부 평가 지표 2";
            case "EXT_SOURCE_3" -> "외부 평가 지표 3";
            case "AMT_CREDIT" -> "신청 신용 금액";
            case "AMT_INCOME_TOTAL" -> "총소득";
            case "AMT_ANNUITY" -> "정기 상환 금액";
            case "AMT_GOODS_PRICE" -> "상품 금액";
            case "NAME_CONTRACT_TYPE" -> "계약 유형";
            case "DAYS_EMPLOYED" -> "재직 기간 관련 정보";
            case "DAYS_BIRTH" -> "연령 관련 정보";
            case "AGE" -> "연령";
            case "AGE_GROUP" -> "연령대";
            default -> feature;
        };
    }
}