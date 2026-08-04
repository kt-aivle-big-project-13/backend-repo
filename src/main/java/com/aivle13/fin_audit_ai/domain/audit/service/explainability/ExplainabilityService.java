package com.aivle13.fin_audit_ai.domain.audit.service.explainability;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.explainability.ExplainabilityResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.request.explainability.ExplainabilityResultRequest;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.ShapFeatureImportanceEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.ShapFeatureImportanceRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiStatus;
import com.aivle13.fin_audit_ai.global.config.CacheConfig;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotCompletedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.ExplainabilityResultNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
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
    private final ShapFeatureImportanceRepository shapFeatureImportanceRepository;
    private final CacheManager cacheManager;

    @Cacheable(cacheNames = CacheConfig.EXPLAINABILITY_CACHE, key = "#userId + ':' + #auditId")
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

        List<ShapFeatureImportanceEntity> topFeatures =
                shapFeatureImportanceRepository.findAllByAudit_IdOrderByRankAsc(auditId);

        return ExplainabilityResponse.of(auditId, results, topFeatures);
    }

    // 취소 직후 재시도가 있었으면(제3의 요청) 이 결과는 이미 지나가버린 실행 세대의
    // 늦은 응답일 수 있다. 그런 경우 저장 자체를 건너뛰어 지금 실행 중인 세대의
    // 결과와 섞이지 않게 한다. 쓰기 잠금으로 조회해 취소/재시도와의 경합도 막는다.
    @Transactional
    public void saveExplainabilityResult(
            Long auditId,
            int generation,
            ExplainabilityResultRequest request
    ) {
        AuditEntity audit = auditRepository.findByIdForUpdate(auditId)
                .orElseThrow(AuditNotFoundException::new);

        if (audit.isCancelled() || audit.getGeneration() != generation) {
            return;
        }

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

        saveFeatureImportances(audit, auditId, request.report());

        evictCache(audit.getUser().getId(), auditId);
    }

    // report는 ShapAnalysisRequest.includeReport=true로 요청했을 때만 채워진다(예외 상황이면 null일 수 있음).
    private void saveFeatureImportances(
            AuditEntity audit,
            Long auditId,
            ExplainabilityResultRequest.ShapReport report
    ) {
        shapFeatureImportanceRepository.deleteAllByAudit_Id(auditId);
        shapFeatureImportanceRepository.flush();

        if (report == null || report.globalImportanceTop() == null) {
            return;
        }

        List<ShapFeatureImportanceEntity> importances = report.globalImportanceTop().stream()
                .map(item -> ShapFeatureImportanceEntity.of(
                        audit,
                        item.rank(),
                        item.feature(),
                        item.meanAbsShap(),
                        item.meanSignedShap(),
                        item.contributionRatio(),
                        item.direction(),
                        Boolean.TRUE.equals(item.isSensitive()),
                        item.sensitiveGroup()
                ))
                .toList();

        shapFeatureImportanceRepository.saveAll(importances);
    }

    private void evictCache(Long userId, Long auditId) {
        Cache cache = cacheManager.getCache(CacheConfig.EXPLAINABILITY_CACHE);

        if (cache != null) {
            cache.evict(userId + ":" + auditId);
        }
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
            case "WARNING" -> XaiStatus.WARNING;
            case "REVIEW" -> XaiStatus.REVIEW;
            default -> throw new AuditFailedException();
        };
    }
}