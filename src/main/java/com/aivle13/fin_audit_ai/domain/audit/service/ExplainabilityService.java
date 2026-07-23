package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.ExplainabilityResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.request.ExplainabilityResultRequest;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiStatus;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotCompletedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.ExplainabilityResultNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExplainabilityService {

    private static final Set<XaiMetricCode> REQUIRED_METRICS = Set.of(
            XaiMetricCode.SENSITIVE_CONTRIB,
            XaiMetricCode.GLOBAL_STABILITY,
            XaiMetricCode.FIDELITY
    );

    private final AuditRepository auditRepository;
    private final XaiResultRepository xaiResultRepository;

    public ExplainabilityResponse getExplainability(
            Long userId,
            Long auditId
    ) {
        AuditEntity audit = auditRepository
                .findByIdAndUser_Id(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);

        if (audit.getStatus() == AuditStatus.PENDING) {
            throw new AuditNotCompletedException();
        }

        if (audit.getStatus() == AuditStatus.FAILED) {
            throw new AuditFailedException();
        }

        List<XaiResultEntity> results =
                xaiResultRepository.findAllByAudit_IdAndMetricCodeIn(
                        auditId,
                        REQUIRED_METRICS
                );

        Set<XaiMetricCode> resultMetricCodes = results.stream()
                .map(XaiResultEntity::getMetricCode)
                .collect(Collectors.toSet());

        if (results.size() != REQUIRED_METRICS.size()
                || !resultMetricCodes.equals(REQUIRED_METRICS)) {
            if (audit.getStatus() == AuditStatus.IN_PROGRESS) {
                throw new AuditNotCompletedException();
            }
            throw new ExplainabilityResultNotFoundException();
        }

        return ExplainabilityResponse.of(auditId, results);
    }

    @Transactional
    public void saveExplainabilityResult(
            Long auditId,
            ExplainabilityResultRequest request
    ) {
        AuditEntity audit = auditRepository.findById(auditId)
                .orElseThrow(AuditNotFoundException::new);

        validateCompletedResult(request);

        ExplainabilityResultRequest.KeyMetrics metrics =
                request.keyMetrics();

        List<XaiResultEntity> results = List.of(
                toEntity(
                        audit,
                        XaiMetricCode.SENSITIVE_CONTRIB,
                        metrics.sensitiveContributionRatio()
                ),
                toEntity(
                        audit,
                        XaiMetricCode.GLOBAL_STABILITY,
                        metrics.globalExplanationStability()
                ),
                toEntity(
                        audit,
                        XaiMetricCode.FIDELITY,
                        metrics.explanationFidelity()
                )
        );

        xaiResultRepository.deleteAllByAudit_IdAndMetricCodeIn(
                auditId,
                REQUIRED_METRICS
        );

        xaiResultRepository.flush();

        xaiResultRepository.saveAll(results);
    }

    private void validateCompletedResult(
            ExplainabilityResultRequest request
    ) {
        if (request == null
                || !"COMPLETED".equalsIgnoreCase(request.pipelineStatus())
                || request.keyMetrics() == null) {
            throw new AuditNotCompletedException();
        }
    }

    private XaiResultEntity toEntity(
            AuditEntity audit,
            XaiMetricCode metricCode,
            ExplainabilityResultRequest.Metric metric
    ) {
        if (metric == null
                || metric.value() == null
                || metric.threshold() == null
                || metric.status() == null) {
            throw new AuditFailedException();
        }

        return XaiResultEntity.of(
                audit,
                metricCode,
                metric.value(),
                metric.threshold(),
                mapStatus(metric.status())
        );
    }

    private XaiStatus mapStatus(String status) {
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "PASS" -> XaiStatus.PASS;
            case "WARNING", "REVIEW" -> XaiStatus.REVIEW;
            default -> throw new AuditFailedException();
        };
    }
}