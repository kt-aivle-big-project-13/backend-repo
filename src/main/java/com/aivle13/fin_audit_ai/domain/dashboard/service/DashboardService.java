package com.aivle13.fin_audit_ai.domain.dashboard.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiStatus;
import com.aivle13.fin_audit_ai.domain.dashboard.dto.response.AuditResultDistributionResponse;
import com.aivle13.fin_audit_ai.domain.dashboard.dto.response.DashboardResponse;
import com.aivle13.fin_audit_ai.domain.dashboard.dto.response.DashboardSummaryResponse;
import com.aivle13.fin_audit_ai.domain.dashboard.dto.response.FairnessMetricDistributionResponse;
import com.aivle13.fin_audit_ai.domain.dashboard.dto.response.RecentAuditResponse;
import com.aivle13.fin_audit_ai.domain.dashboard.dto.response.ReviewRequiredModelResponse;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.DashboardQueryRepository;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.AuditStatusCountProjection;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.DashboardSummaryProjection;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.RecentAuditProjection;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.ReviewRequiredModelProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int REVIEW_REQUIRED_MODEL_LIMIT = 5;

    private final DashboardQueryRepository dashboardQueryRepository;

    // 대시보드의 모든 영역을 로그인한 사용자 소유의 감사만으로 조회해 하나의 응답으로 조립한다.
    public DashboardResponse getDashboard(Long userId) {
        DashboardSummaryResponse summary = createSummary(userId);
        AuditResultDistributionResponse auditDistribution =
                createAuditResultDistribution(userId, summary.analyzedModelCount());
        List<ReviewRequiredModelResponse> topModels =
                createReviewRequiredTopModels(userId);
        List<FairnessMetricDistributionResponse> fairnessDistributions =
                createFairnessMetricDistributions(userId, summary.analyzedModelCount());
        List<RecentAuditResponse> recentAudits = createRecentAudits(userId);

        return new DashboardResponse(
                summary,
                auditDistribution,
                topModels,
                fairnessDistributions,
                recentAudits
        );
    }

    // 상단 카드의 모델 수를 조회하고 전체 규정 준수율을 계산한다.
    private DashboardSummaryResponse createSummary(Long userId) {
        DashboardSummaryProjection projection =
                dashboardQueryRepository.findSummary(userId);

        long analyzedCount = valueOrZero(projection.getAnalyzedModelCount());
        long normalCount = valueOrZero(projection.getNormalModelCount());
        long reviewCount = valueOrZero(projection.getReviewRequiredCount());
        long exceededCount =
                valueOrZero(projection.getThresholdExceededCount());
        double complianceRate = percentage(normalCount, analyzedCount);

        return new DashboardSummaryResponse(
                analyzedCount,
                normalCount,
                reviewCount,
                exceededCount,
                complianceRate
        );
    }

    // 최신 감사의 종합판정별 모델 수를 도넛 차트 응답으로 변환한다.
    private AuditResultDistributionResponse createAuditResultDistribution(
            Long userId,
            long analyzedModelCount
    ) {
        Map<AuditStatus, Long> counts = new EnumMap<>(AuditStatus.class);

        for (AuditStatusCountProjection projection
                : dashboardQueryRepository.countLatestAuditsByStatus(userId)) {
            counts.put(projection.getStatus(), valueOrZero(projection.getCount()));
        }

        return new AuditResultDistributionResponse(
                analyzedModelCount,
                counts.getOrDefault(AuditStatus.COMPLIANT, 0L),
                counts.getOrDefault(AuditStatus.WARNING, 0L),
                counts.getOrDefault(AuditStatus.NON_COMPLIANT, 0L)
        );
    }

    // 공정성·SHAP 문제 개수를 합산해 검토 필요 모델 상위 5개를 선정한다.
    private List<ReviewRequiredModelResponse>
            createReviewRequiredTopModels(Long userId) {
        Map<Long, ModelIssueSummary> summaries = new HashMap<>();

        mergeIssueCounts(
                summaries,
                dashboardQueryRepository.countFairnessIssuesByModel(userId)
        );
        mergeIssueCounts(
                summaries,
                dashboardQueryRepository.countXaiIssuesByModel(userId)
        );

        return summaries.values().stream()
                .sorted(Comparator
                        .comparingLong(ModelIssueSummary::issueCount)
                        .reversed()
                        .thenComparing(ModelIssueSummary::modelId))
                .limit(REVIEW_REQUIRED_MODEL_LIMIT)
                .map(summary -> new ReviewRequiredModelResponse(
                        summary.modelId(),
                        summary.modelName(),
                        summary.version(),
                        summary.issueCount(),
                        summary.warningCount(),
                        summary.thresholdExceededCount(),
                        summary.status()
                ))
                .toList();
    }

    // 같은 모델의 공정성·SHAP 문제 개수를 하나의 결과로 합친다.
    private void mergeIssueCounts(
            Map<Long, ModelIssueSummary> summaries,
            Collection<ReviewRequiredModelProjection> projections
    ) {
        for (ReviewRequiredModelProjection projection : projections) {
            long issueCount = valueOrZero(projection.getIssueCount());
            long warningCount = valueOrZero(projection.getWarningCount());
            long thresholdExceededCount =
                    valueOrZero(projection.getThresholdExceededCount());

            summaries.merge(
                    projection.getModelId(),
                    new ModelIssueSummary(
                            projection.getModelId(),
                            projection.getModelName(),
                            projection.getVersion(),
                            issueCount,
                            warningCount,
                            thresholdExceededCount,
                            projection.getStatus()
                    ),
                    (existing, incoming) -> new ModelIssueSummary(
                            existing.modelId(),
                            existing.modelName(),
                            existing.version(),
                            existing.issueCount() + incoming.issueCount(),
                            existing.warningCount() + incoming.warningCount(),
                            existing.thresholdExceededCount()
                                    + incoming.thresholdExceededCount(),
                            moreSevere(existing.status(), incoming.status())
                    )
            );
        }
    }

    // 모델별·공정성 지표별 가장 심각한 판정을 기준으로 상태 분포를 계산한다.
    private List<FairnessMetricDistributionResponse>
            createFairnessMetricDistributions(Long userId, long analyzedModelCount) {
        List<FairnessResultEntity> results =
                dashboardQueryRepository.findLatestFairnessResults(userId);

        Map<AuditMetricKey, FairnessStatus> worstStatuses = new HashMap<>();

        for (FairnessResultEntity result : results) {
            AuditMetricKey key = new AuditMetricKey(
                    result.getAudit().getId(),
                    result.getMetricCode()
            );
            worstStatuses.merge(
                    key,
                    result.getStatus(),
                    this::moreSevere
            );
        }

        Map<FairnessMetricCode, Map<FairnessStatus, Long>> counts =
                new EnumMap<>(FairnessMetricCode.class);

        for (Map.Entry<AuditMetricKey, FairnessStatus> entry
                : worstStatuses.entrySet()) {
            counts.computeIfAbsent(
                    entry.getKey().metricCode(),
                    ignored -> new EnumMap<>(FairnessStatus.class)
            ).merge(entry.getValue(), 1L, Long::sum);
        }

        List<FairnessMetricDistributionResponse> distributions =
                new ArrayList<>();

        for (FairnessMetricCode metricCode : FairnessMetricCode.values()) {
            Map<FairnessStatus, Long> statusCounts =
                    counts.getOrDefault(
                            metricCode,
                            new EnumMap<>(FairnessStatus.class)
                    );
            long passCount =
                    statusCounts.getOrDefault(FairnessStatus.PASS, 0L);
            long reviewCount =
                    statusCounts.getOrDefault(FairnessStatus.REVIEW, 0L);
            long failCount =
                    statusCounts.getOrDefault(FairnessStatus.FAIL, 0L);
            long evaluatedCount = passCount + reviewCount + failCount;
            long unavailableCount =
                    Math.max(0L, analyzedModelCount - evaluatedCount);

            distributions.add(new FairnessMetricDistributionResponse(
                    metricCode,
                    passCount,
                    reviewCount,
                    failCount,
                    unavailableCount,
                    percentage(passCount, analyzedModelCount),
                    percentage(reviewCount, analyzedModelCount),
                    percentage(failCount, analyzedModelCount),
                    percentage(unavailableCount, analyzedModelCount)
            ));
        }

        return distributions;
    }

    // 완료된 감사 전체를 최신순으로 조회하고 각 감사의 핵심 위험 신호를 연결한다.
    private List<RecentAuditResponse> createRecentAudits(Long userId) {
        List<RecentAuditProjection> audits =
                dashboardQueryRepository.findRecentAudits(userId);

        if (audits.isEmpty()) {
            return List.of();
        }

        List<Long> auditIds = audits.stream()
                .map(RecentAuditProjection::getAuditId)
                .toList();
        Map<Long, String> keyRisks = createKeyRisks(auditIds);

        return audits.stream()
                .map(audit -> new RecentAuditResponse(
                        audit.getAuditId(),
                        audit.getModelId(),
                        audit.getModelName(),
                        audit.getVersion(),
                        audit.getStatus(),
                        keyRisks.getOrDefault(
                                audit.getAuditId(),
                                "분석 결과 없음"
                        ),
                        audit.getCompletedAt()
                ))
                .toList();
    }

    // 감사별 공정성·SHAP 결과에서 대표 핵심 위험 신호를 생성한다.
    private Map<Long, String> createKeyRisks(List<Long> auditIds) {
        Map<Long, RiskSignal> risks = new HashMap<>();
        Map<Long, Boolean> hasResult = new HashMap<>();

        for (FairnessResultEntity result
                : dashboardQueryRepository
                        .findFairnessResultsByAuditIds(auditIds)) {
            Long auditId = result.getAudit().getId();
            hasResult.put(auditId, true);

            if (result.getStatus() != FairnessStatus.PASS) {
                mergeRisk(risks, auditId, fairnessRisk(result));
            }
        }

        for (XaiResultEntity result
                : dashboardQueryRepository.findXaiResultsByAuditIds(auditIds)) {
            Long auditId = result.getAudit().getId();
            hasResult.put(auditId, true);

            if (result.getStatus() != XaiStatus.PASS) {
                mergeRisk(risks, auditId, xaiRisk(result));
            }
        }

        Map<Long, String> keyRisks = new HashMap<>();

        for (Long auditId : auditIds) {
            RiskSignal risk = risks.get(auditId);

            if (risk != null) {
                keyRisks.put(auditId, risk.description());
            } else if (hasResult.containsKey(auditId)) {
                keyRisks.put(auditId, "이상 신호 없음");
            } else {
                keyRisks.put(auditId, "분석 결과 없음");
            }
        }

        return keyRisks;
    }

    // 기존 위험과 새 위험을 비교해 더 심각한 결과만 남긴다.
    private void mergeRisk(
            Map<Long, RiskSignal> risks,
            Long auditId,
            RiskSignal candidate
    ) {
        risks.merge(
                auditId,
                candidate,
                (current, incoming) ->
                        RISK_COMPARATOR.compare(incoming, current) > 0
                                ? incoming
                                : current
        );
    }

    // 공정성 결과의 심각도와 임계값 이탈 정도를 핵심 위험 후보로 변환한다.
    private RiskSignal fairnessRisk(FairnessResultEntity result) {
        boolean minimumMetric =
                result.getMetricCode()
                        == FairnessMetricCode.PROPORTIONAL_PARITY;
        int severity = switch (result.getStatus()) {
            case FAIL -> 3;
            case REVIEW -> 2;
            case PASS -> 0;
        };
        double deviation = normalizedDeviation(
                result.getValue(),
                result.getThreshold(),
                minimumMetric
        );
        String direction = minimumMetric ? "미달" : "초과";
        String description = result.getAttribute()
                + " " + result.getMetricCode().name()
                + " " + number(result.getValue())
                + " — 기준 " + number(result.getThreshold())
                + " " + direction;

        return new RiskSignal(severity, deviation, description);
    }

    // SHAP 결과의 심각도와 임계값 이탈 정도를 핵심 위험 후보로 변환한다.
    private RiskSignal xaiRisk(XaiResultEntity result) {
        boolean minimumMetric =
                result.getMetricCode() != XaiMetricCode.SENSITIVE_CONTRIB;
        int severity = switch (result.getStatus()) {
            case REVIEW -> 2;
            case WARNING -> 1;
            case PASS -> 0;
        };
        double deviation = normalizedDeviation(
                result.getValue(),
                result.getThreshold(),
                minimumMetric
        );
        String direction = minimumMetric ? "미달" : "초과";
        String description = result.getMetricCode().name()
                + " " + number(result.getValue())
                + " — 기준 " + number(result.getThreshold())
                + " " + direction;

        return new RiskSignal(severity, deviation, description);
    }

    // 지표의 판정 방향을 고려해 임계값 대비 이탈 비율을 계산한다.
    private double normalizedDeviation(
            BigDecimal value,
            BigDecimal threshold,
            boolean minimumMetric
    ) {
        if (threshold.signum() == 0) {
            return value.abs().doubleValue();
        }

        BigDecimal difference = minimumMetric
                ? threshold.subtract(value)
                : value.abs().subtract(threshold);

        return difference.max(BigDecimal.ZERO)
                .divide(threshold.abs(), 8, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // 두 공정성 판정 중 더 심각한 상태를 반환한다.
    private FairnessStatus moreSevere(
            FairnessStatus first,
            FairnessStatus second
    ) {
        return fairnessSeverity(first) >= fairnessSeverity(second)
                ? first
                : second;
    }

    // 두 감사 종합판정 중 더 심각한 상태를 반환한다.
    private AuditStatus moreSevere(
            AuditStatus first,
            AuditStatus second
    ) {
        return auditSeverity(first) >= auditSeverity(second)
                ? first
                : second;
    }

    // 공정성 판정을 비교 가능한 심각도 숫자로 변환한다.
    private int fairnessSeverity(FairnessStatus status) {
        return switch (status) {
            case PASS -> 0;
            case REVIEW -> 1;
            case FAIL -> 2;
        };
    }

    // 감사 종합판정을 비교 가능한 심각도 숫자로 변환한다.
    private int auditSeverity(AuditStatus status) {
        return switch (status) {
            case NON_COMPLIANT -> 2;
            case WARNING -> 1;
            default -> 0;
        };
    }

    // 집계 결과가 null이면 0으로 변환한다.
    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    // 개수를 소수점 첫째 자리의 백분율로 변환한다.
    private double percentage(long value, long total) {
        if (total == 0L) {
            return 0.0;
        }

        return Math.round((value * 1000.0) / total) / 10.0;
    }

    // 지표값의 불필요한 소수점 0을 제거해 화면용 문자열로 변환한다.
    private String number(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    // 모델별 문제 개수와 종합판정을 합산하기 위한 내부 데이터다.
    private record ModelIssueSummary(
            Long modelId,
            String modelName,
            String version,
            long issueCount,
            long warningCount,
            long thresholdExceededCount,
            AuditStatus status
    ) {
    }

    // 감사와 공정성 지표 조합을 구분하기 위한 내부 키다.
    private record AuditMetricKey(
            Long auditId,
            FairnessMetricCode metricCode
    ) {
    }

    // 핵심 위험 후보의 심각도와 임계값 이탈 정도를 보관한다.
    private record RiskSignal(
            int severity,
            double normalizedDeviation,
            String description
    ) {
    }

    // 심각도를 먼저 비교하고 같으면 임계값 이탈 정도를 비교한다.
    private static final Comparator<RiskSignal> RISK_COMPARATOR =
            Comparator.comparingInt(RiskSignal::severity)
                    .thenComparingDouble(RiskSignal::normalizedDeviation);
}
