package com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness;

import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessGroupStatEntity;

import java.math.BigDecimal;

/**
 * 보호속성별 집단 하나의 기초 통계·혼동행렬 응답.
 * 혼동행렬은 favorable(승인=유리) 관점 — 정상 승인=tp, 연체 승인=fp,
 * 연체 거절=tn, 정상 거절=fn.
 */
public record FairnessGroupStatResponse(
        String attribute,
        String group,
        int n,
        BigDecimal approvalRate,
        BigDecimal actualDefaultRate,
        int tp,
        int fp,
        int tn,
        int fn,
        BigDecimal auc
) {

    public static FairnessGroupStatResponse from(FairnessGroupStatEntity stat) {
        return new FairnessGroupStatResponse(
                stat.getAttribute(),
                stat.getGroupName(),
                stat.getN(),
                stat.getApprovalRate(),
                stat.getActualDefaultRate(),
                stat.getTp(),
                stat.getFp(),
                stat.getTn(),
                stat.getFn(),
                stat.getAuc()
        );
    }
}
