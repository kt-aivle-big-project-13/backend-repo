package com.aivle13.fin_audit_ai.domain.demo.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessGroupStatEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.ShapFeatureImportanceEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * 시연용 완료 감사 결과.
 *
 * <p>실제 분석을 돌리지 않고 결과 화면을 보여주기 위한 고정값이다. AI 서버는 분석 2건·
 * 리포트 1건까지만 동시에 처리하므로, 여러 명이 한꺼번에 감사를 돌리면 뒤쪽은 오래
 * 기다리거나 503 을 받는다. 결과를 미리 넣어 두면 화면을 대기 없이 볼 수 있고, 실제
 * 실행은 원하는 사람만 하게 된다.
 *
 * <p>값은 특정 모델의 실제 감사 결과가 아니라 시연용으로 정한 것이다. 지표 간 방향이
 * 서로 어긋나지 않도록(예: 승인율 격차가 큰 집단의 지표가 PASS 로 나오지 않도록) 맞춰
 * 두었다.
 */
final class DemoAuditFixture {

    static final String AUDIT_NAME = "샘플 신용평가 모델 감사";
    static final String MODEL_NAME = "샘플 신용평가 모델";
    static final String MODEL_VERSION = "v1.0";

    // 승인율 격차가 성별에서 두드러지게 잡히도록 구성했다. 화면에서 "왜 주의인지"가
    // 집단 통계와 함께 읽히게 하려는 것이다.
    private static final String GENDER = "CODE_GENDER";
    private static final String AGE_GROUP = "AGE_GROUP";

    private DemoAuditFixture() {
    }

    static List<FairnessResultEntity> fairnessResults(AuditEntity audit) {
        return List.of(
                FairnessResultEntity.of(audit, GENDER, FairnessMetricCode.DEMOGRAPHIC_PARITY,
                        decimal("0.1120"), decimal("0.1000"), FairnessStatus.REVIEW),
                FairnessResultEntity.of(audit, GENDER, FairnessMetricCode.EQUAL_OPPORTUNITY,
                        decimal("0.0870"), decimal("0.1000"), FairnessStatus.PASS),
                FairnessResultEntity.of(audit, GENDER, FairnessMetricCode.EQUALIZED_ODDS,
                        decimal("0.1340"), decimal("0.1000"), FairnessStatus.REVIEW),
                FairnessResultEntity.of(audit, AGE_GROUP, FairnessMetricCode.DEMOGRAPHIC_PARITY,
                        decimal("0.0640"), decimal("0.1000"), FairnessStatus.PASS),
                FairnessResultEntity.of(audit, AGE_GROUP, FairnessMetricCode.EQUAL_OPPORTUNITY,
                        decimal("0.0510"), decimal("0.1000"), FairnessStatus.PASS),
                FairnessResultEntity.of(audit, AGE_GROUP, FairnessMetricCode.EQUALIZED_ODDS,
                        decimal("0.0730"), decimal("0.1000"), FairnessStatus.PASS)
        );
    }

    static List<FairnessGroupStatEntity> groupStats(AuditEntity audit) {
        return List.of(
                // 성별: 승인율 차이 0.112 가 아래 두 집단의 차이와 맞아떨어진다.
                FairnessGroupStatEntity.of(audit, GENDER, "M", 5120,
                        decimal("0.7840"), decimal("0.0910"), 388, 712, 3623, 397, decimal("0.7420")),
                FairnessGroupStatEntity.of(audit, GENDER, "F", 4880,
                        decimal("0.8960"), decimal("0.0620"), 141, 366, 4071, 302, decimal("0.7510")),

                FairnessGroupStatEntity.of(audit, AGE_GROUP, "20-39", 3640,
                        decimal("0.8110"), decimal("0.0880"), 205, 481, 2748, 206, decimal("0.7380")),
                FairnessGroupStatEntity.of(audit, AGE_GROUP, "40-59", 4720,
                        decimal("0.8620"), decimal("0.0700"), 174, 470, 3906, 170, decimal("0.7490")),
                FairnessGroupStatEntity.of(audit, AGE_GROUP, "60+", 1640,
                        decimal("0.8470"), decimal("0.0760"), 66, 185, 1330, 59, decimal("0.7440"))
        );
    }

    static List<XaiResultEntity> xaiResults(AuditEntity audit) {
        return List.of(
                XaiResultEntity.of(audit, XaiMetricCode.SENSITIVE_CONTRIB,
                        decimal("0.1480"), decimal("0.2000"), XaiStatus.PASS),
                XaiResultEntity.of(audit, XaiMetricCode.GLOBAL_STABILITY,
                        decimal("0.8930"), decimal("0.7000"), XaiStatus.PASS),
                XaiResultEntity.of(audit, XaiMetricCode.FIDELITY,
                        decimal("0.6120"), decimal("0.5000"), XaiStatus.PASS)
        );
    }

    static List<ShapFeatureImportanceEntity> shapFeatures(AuditEntity audit) {
        return List.of(
                shap(audit, 1, "EXT_SOURCE_2", "0.8210", "-0.4120", "0.1840", "RISK_DECREASE", false, null),
                shap(audit, 2, "EXT_SOURCE_3", "0.7640", "-0.3880", "0.1710", "RISK_DECREASE", false, null),
                shap(audit, 3, "AMT_CREDIT", "0.5130", "0.2470", "0.1150", "RISK_INCREASE", false, null),
                shap(audit, 4, "DAYS_EMPLOYED", "0.4420", "-0.1980", "0.0990", "RISK_DECREASE", false, null),
                shap(audit, 5, "AMT_INCOME_TOTAL", "0.3870", "-0.1640", "0.0870", "RISK_DECREASE", false, null),
                shap(audit, 6, "CODE_GENDER", "0.3310", "0.1520", "0.0740", "RISK_INCREASE", true, "gender"),
                shap(audit, 7, "DAYS_BIRTH", "0.2980", "-0.1210", "0.0670", "RISK_DECREASE", true, "age"),
                shap(audit, 8, "AMT_ANNUITY", "0.2440", "0.0980", "0.0550", "RISK_INCREASE", false, null),
                shap(audit, 9, "NAME_EDUCATION_TYPE", "0.1870", "-0.0710", "0.0420", "RISK_DECREASE", false, null),
                shap(audit, 10, "OWN_CAR_AGE", "0.1520", "0.0590", "0.0340", "RISK_INCREASE", false, null)
        );
    }

    private static ShapFeatureImportanceEntity shap(
            AuditEntity audit, int rank, String feature, String meanAbs, String meanSigned,
            String ratio, String direction, boolean sensitive, String sensitiveGroup
    ) {
        return ShapFeatureImportanceEntity.of(
                audit, rank, feature,
                decimal(meanAbs), decimal(meanSigned), decimal(ratio),
                direction, sensitive, sensitiveGroup
        );
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
