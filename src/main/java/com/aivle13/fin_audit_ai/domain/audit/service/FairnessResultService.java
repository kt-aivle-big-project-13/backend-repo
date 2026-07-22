package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.FairnessResultResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.FairnessRunResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessStatus;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotCompletedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.FairnessResultNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FairnessResultService {

    // TODO: 정책값 미확정. AI팀/기획 확정 후 조정 필요
    private static final BigDecimal DEMOGRAPHIC_PARITY_THRESHOLD = new BigDecimal("0.10");
    private static final BigDecimal EQUAL_OPPORTUNITY_THRESHOLD = new BigDecimal("0.10");
    private static final BigDecimal EQUALIZED_ODDS_THRESHOLD = new BigDecimal("0.10");
    private static final BigDecimal REVIEW_THRESHOLD_MULTIPLIER = BigDecimal.valueOf(2);

    private final AuditRepository auditRepository;
    private final FairnessResultRepository fairnessResultRepository;

    public FairnessResultResponse getFairness(Long userId, Long auditId) {
        AuditEntity audit = auditRepository
                .findByIdAndUser_Id(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);

        if (audit.getStatus() == AuditStatus.IN_PROGRESS) {
            throw new AuditNotCompletedException();
        }

        List<FairnessResultEntity> results =
                fairnessResultRepository.findAllByAudit_Id(auditId);

        if (results.isEmpty()) {
            throw new FairnessResultNotFoundException();
        }

        return FairnessResultResponse.of(auditId, results);
    }

    @Transactional
    public void saveFairnessResult(Long auditId, FairnessRunResponse response) {
        AuditEntity audit = auditRepository.findById(auditId)
                .orElseThrow(AuditNotFoundException::new);

        validateResponse(response);

        List<FairnessResultEntity> results = new ArrayList<>();

        for (Map.Entry<String, FairnessRunResponse.AttributeFairness> entry
                : response.fairnessByAttribute().entrySet()) {

            String attribute = entry.getKey();
            FairnessRunResponse.AttributeFairness fairness = entry.getValue();

            results.add(toEntity(
                    audit,
                    attribute,
                    FairnessMetricCode.DEMOGRAPHIC_PARITY,
                    fairness.demographicParityDifference(),
                    DEMOGRAPHIC_PARITY_THRESHOLD
            ));

            results.add(toEntity(
                    audit,
                    attribute,
                    FairnessMetricCode.EQUAL_OPPORTUNITY,
                    fairness.equalOpportunityDifference(),
                    EQUAL_OPPORTUNITY_THRESHOLD
            ));

            results.add(toEntity(
                    audit,
                    attribute,
                    FairnessMetricCode.EQUALIZED_ODDS,
                    fairness.equalizedOddsDifference(),
                    EQUALIZED_ODDS_THRESHOLD
            ));
        }

        fairnessResultRepository.deleteAllByAudit_Id(auditId);
        fairnessResultRepository.saveAll(results);
    }

    private void validateResponse(FairnessRunResponse response) {
        if (response == null
                || response.fairnessByAttribute() == null
                || response.fairnessByAttribute().isEmpty()) {
            throw new AuditFailedException();
        }
    }

    private FairnessResultEntity toEntity(
            AuditEntity audit,
            String attribute,
            FairnessMetricCode metricCode,
            BigDecimal value,
            BigDecimal threshold
    ) {
        if (value == null) {
            throw new AuditFailedException();
        }

        return FairnessResultEntity.of(
                audit,
                attribute,
                metricCode,
                value,
                threshold,
                judgeStatus(value.abs(), threshold)
        );
    }

    // TODO: 정책값 미확정. AI팀/기획 확정 후 조정 필요
    private FairnessStatus judgeStatus(BigDecimal absValue, BigDecimal threshold) {
        if (absValue.compareTo(threshold) <= 0) {
            return FairnessStatus.PASS;
        }

        if (absValue.compareTo(threshold.multiply(REVIEW_THRESHOLD_MULTIPLIER)) <= 0) {
            return FairnessStatus.REVIEW;
        }

        return FairnessStatus.FAIL;
    }
}
