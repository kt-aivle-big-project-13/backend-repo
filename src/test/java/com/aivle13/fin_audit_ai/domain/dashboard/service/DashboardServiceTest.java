package com.aivle13.fin_audit_ai.domain.dashboard.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessStatus;
import com.aivle13.fin_audit_ai.domain.dashboard.dto.response.DashboardResponse;
import com.aivle13.fin_audit_ai.domain.dashboard.dto.response.FairnessMetricDistributionResponse;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.DashboardQueryRepository;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.AuditStatusCountProjection;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.DashboardSummaryProjection;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.RecentAuditProjection;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.ReviewRequiredModelProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

// DashboardService.getDashboard()는 요약·분포·TOP5·지표분포·최근감사를 한 번에 조립하므로,
// 어떤 케이스든 아래 6개 리포지토리 호출은 항상 실행된다. stubDashboard()로 그때그때
// 필요한 값만 채우고 나머지는 빈 값/0으로 둔다.
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardQueryRepository dashboardQueryRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private void stubDashboard(
            DashboardSummaryProjection summary,
            List<AuditStatusCountProjection> auditStatusCounts,
            List<ReviewRequiredModelProjection> fairnessIssues,
            List<ReviewRequiredModelProjection> xaiIssues,
            List<FairnessResultEntity> latestFairnessResults,
            List<RecentAuditProjection> recentAudits
    ) {
        given(dashboardQueryRepository.findSummary()).willReturn(summary);
        given(dashboardQueryRepository.countLatestAuditsByStatus()).willReturn(auditStatusCounts);
        given(dashboardQueryRepository.countFairnessIssuesByModel()).willReturn(fairnessIssues);
        given(dashboardQueryRepository.countXaiIssuesByModel()).willReturn(xaiIssues);
        given(dashboardQueryRepository.findLatestFairnessResults()).willReturn(latestFairnessResults);
        given(dashboardQueryRepository.findRecentAudits(any())).willReturn(recentAudits);
    }

    private DashboardSummaryProjection summary(long analyzed, long normal, long review, long exceeded) {
        DashboardSummaryProjection projection = mock(DashboardSummaryProjection.class);
        given(projection.getAnalyzedModelCount()).willReturn(analyzed);
        given(projection.getNormalModelCount()).willReturn(normal);
        given(projection.getReviewRequiredCount()).willReturn(review);
        given(projection.getThresholdExceededCount()).willReturn(exceeded);
        return projection;
    }

    private AuditStatusCountProjection statusCount(AuditStatus status, long count) {
        AuditStatusCountProjection projection = mock(AuditStatusCountProjection.class);
        given(projection.getStatus()).willReturn(status);
        given(projection.getCount()).willReturn(count);
        return projection;
    }

    private ReviewRequiredModelProjection reviewRequired(
            Long modelId, String name, String version, long issueCount, AuditStatus status
    ) {
        ReviewRequiredModelProjection projection = mock(ReviewRequiredModelProjection.class);
        given(projection.getModelId()).willReturn(modelId);
        given(projection.getModelName()).willReturn(name);
        given(projection.getVersion()).willReturn(version);
        given(projection.getIssueCount()).willReturn(issueCount);
        given(projection.getStatus()).willReturn(status);
        return projection;
    }

    private RecentAuditProjection recentAudit(
            Long auditId, Long modelId, String modelName, String version, AuditStatus status, LocalDateTime completedAt
    ) {
        RecentAuditProjection projection = mock(RecentAuditProjection.class);
        given(projection.getAuditId()).willReturn(auditId);
        given(projection.getModelId()).willReturn(modelId);
        given(projection.getModelName()).willReturn(modelName);
        given(projection.getVersion()).willReturn(version);
        given(projection.getStatus()).willReturn(status);
        given(projection.getCompletedAt()).willReturn(completedAt);
        return projection;
    }

    private AuditEntity audit(Long id) {
        AuditEntity audit = mock(AuditEntity.class);
        given(audit.getId()).willReturn(id);
        return audit;
    }

    @Test
    void 상단_요약의_규정준수율을_정상_모델_비율로_계산한다() {
        stubDashboard(summary(10, 7, 2, 1), List.of(), List.of(), List.of(), List.of(), List.of());

        DashboardResponse result = dashboardService.getDashboard();

        assertThat(result.summary().analyzedModelCount()).isEqualTo(10);
        assertThat(result.summary().complianceRate()).isEqualTo(70.0);
    }

    @Test
    void 분석된_모델이_없으면_규정준수율은_0이다() {
        stubDashboard(summary(0, 0, 0, 0), List.of(), List.of(), List.of(), List.of(), List.of());

        DashboardResponse result = dashboardService.getDashboard();

        assertThat(result.summary().complianceRate()).isEqualTo(0.0);
    }

    @Test
    void 최신_감사의_종합판정별_모델_수를_분포로_반환한다() {
        stubDashboard(
                summary(10, 6, 3, 1),
                List.of(
                        statusCount(AuditStatus.COMPLIANT, 6),
                        statusCount(AuditStatus.WARNING, 3),
                        statusCount(AuditStatus.NON_COMPLIANT, 1)
                ),
                List.of(), List.of(), List.of(), List.of()
        );

        DashboardResponse result = dashboardService.getDashboard();

        assertThat(result.auditResultDistribution().totalCount()).isEqualTo(10);
        assertThat(result.auditResultDistribution().normalCount()).isEqualTo(6);
        assertThat(result.auditResultDistribution().reviewRequiredCount()).isEqualTo(3);
        assertThat(result.auditResultDistribution().thresholdExceededCount()).isEqualTo(1);
    }

    @Test
    void 공정성과_SHAP_문제_개수를_합산해_검토_필요_모델_상위_5개를_선정한다() {
        List<ReviewRequiredModelProjection> fairnessIssues = List.of(
                reviewRequired(1L, "모델1", "1.0", 6, AuditStatus.WARNING),
                reviewRequired(2L, "모델2", "1.0", 5, AuditStatus.WARNING),
                reviewRequired(3L, "모델3", "1.0", 4, AuditStatus.WARNING),
                reviewRequired(4L, "모델4", "1.0", 3, AuditStatus.WARNING),
                reviewRequired(5L, "모델5", "1.0", 2, AuditStatus.WARNING),
                reviewRequired(6L, "모델6", "1.0", 1, AuditStatus.WARNING)
        );
        stubDashboard(summary(6, 0, 6, 0), List.of(), fairnessIssues, List.of(), List.of(), List.of());

        DashboardResponse result = dashboardService.getDashboard();

        assertThat(result.reviewRequiredTopModels()).hasSize(5);
        assertThat(result.reviewRequiredTopModels().get(0).modelId()).isEqualTo(1L);
        assertThat(result.reviewRequiredTopModels().get(4).modelId()).isEqualTo(5L);
        assertThat(result.reviewRequiredTopModels())
                .extracting(response -> response.modelId())
                .doesNotContain(6L);
    }

    @Test
    void SHAP_문제가_합산되면_공정성_문제만으로는_5위_밖이던_모델이_상위로_올라온다() {
        List<ReviewRequiredModelProjection> fairnessIssues = List.of(
                reviewRequired(1L, "모델1", "1.0", 6, AuditStatus.WARNING),
                reviewRequired(2L, "모델2", "1.0", 5, AuditStatus.WARNING),
                reviewRequired(3L, "모델3", "1.0", 4, AuditStatus.WARNING),
                reviewRequired(4L, "모델4", "1.0", 3, AuditStatus.WARNING),
                reviewRequired(5L, "모델5", "1.0", 2, AuditStatus.WARNING),
                // 공정성 문제만 보면 1건뿐이라 상위 5개(1~5위)에 들지 못하는 모델.
                reviewRequired(6L, "모델6", "1.0", 1, AuditStatus.WARNING)
        );
        // 모델6에 SHAP 문제 10건이 추가로 합산되면 총합이 11건이 되어 6위(제외 대상)에서 1위로 올라오고,
        // 대신 공정성만으로 5위였던 모델5(2건)가 5위 밖으로 밀려난다.
        List<ReviewRequiredModelProjection> xaiIssues =
                List.of(reviewRequired(6L, "모델6", "1.0", 10, AuditStatus.NON_COMPLIANT));
        stubDashboard(summary(6, 0, 6, 0), List.of(), fairnessIssues, xaiIssues, List.of(), List.of());

        DashboardResponse result = dashboardService.getDashboard();

        assertThat(result.reviewRequiredTopModels()).hasSize(5);
        assertThat(result.reviewRequiredTopModels().get(0).modelId()).isEqualTo(6L);
        assertThat(result.reviewRequiredTopModels().get(0).issueCount()).isEqualTo(11);
        assertThat(result.reviewRequiredTopModels())
                .extracting(response -> response.modelId())
                .doesNotContain(5L);
    }

    @Test
    void 같은_모델의_공정성과_SHAP_문제는_더_심각한_상태로_합쳐진다() {
        List<ReviewRequiredModelProjection> fairnessIssues =
                List.of(reviewRequired(100L, "모델A", "1.0", 2, AuditStatus.WARNING));
        List<ReviewRequiredModelProjection> xaiIssues =
                List.of(reviewRequired(100L, "모델A", "1.0", 3, AuditStatus.NON_COMPLIANT));
        stubDashboard(summary(1, 0, 1, 0), List.of(), fairnessIssues, xaiIssues, List.of(), List.of());

        DashboardResponse result = dashboardService.getDashboard();

        assertThat(result.reviewRequiredTopModels()).hasSize(1);
        assertThat(result.reviewRequiredTopModels().get(0).issueCount()).isEqualTo(5);
        assertThat(result.reviewRequiredTopModels().get(0).status()).isEqualTo(AuditStatus.NON_COMPLIANT);
    }

    @Test
    void 공정성_지표별_상태_분포를_모든_지표_코드에_대해_반환한다() {
        AuditEntity audit1 = audit(1L);
        AuditEntity audit2 = audit(2L);
        List<FairnessResultEntity> latestFairnessResults = List.of(
                FairnessResultEntity.of(audit1, "GENDER", FairnessMetricCode.DEMOGRAPHIC_PARITY,
                        new BigDecimal("0.10"), new BigDecimal("0.20"), FairnessStatus.PASS),
                FairnessResultEntity.of(audit2, "GENDER", FairnessMetricCode.DEMOGRAPHIC_PARITY,
                        new BigDecimal("0.30"), new BigDecimal("0.20"), FairnessStatus.REVIEW),
                FairnessResultEntity.of(audit1, "AGE_GROUP", FairnessMetricCode.EQUAL_OPPORTUNITY,
                        new BigDecimal("0.35"), new BigDecimal("0.20"), FairnessStatus.FAIL)
        );
        stubDashboard(summary(2, 0, 0, 0), List.of(), List.of(), List.of(), latestFairnessResults, List.of());

        DashboardResponse result = dashboardService.getDashboard();

        assertThat(result.fairnessMetricDistributions()).hasSize(FairnessMetricCode.values().length);

        FairnessMetricDistributionResponse demographicParity = result.fairnessMetricDistributions().stream()
                .filter(d -> d.metricCode() == FairnessMetricCode.DEMOGRAPHIC_PARITY)
                .findFirst().orElseThrow();
        assertThat(demographicParity.passCount()).isEqualTo(1);
        assertThat(demographicParity.reviewCount()).isEqualTo(1);
        assertThat(demographicParity.failCount()).isEqualTo(0);
        assertThat(demographicParity.unavailableCount()).isEqualTo(0);
        assertThat(demographicParity.passRate()).isEqualTo(50.0);
        assertThat(demographicParity.reviewRate()).isEqualTo(50.0);

        FairnessMetricDistributionResponse noData = result.fairnessMetricDistributions().stream()
                .filter(d -> d.metricCode() == FairnessMetricCode.EQUALIZED_ODDS)
                .findFirst().orElseThrow();
        assertThat(noData.unavailableCount()).isEqualTo(2);
        assertThat(noData.unavailableRate()).isEqualTo(100.0);
    }

    @Test
    void 최근_완료_감사_5건을_핵심_위험_신호와_함께_반환한다() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        List<RecentAuditProjection> recentAudits = List.of(
                recentAudit(1L, 10L, "모델A", "1.0", AuditStatus.NON_COMPLIANT, completedAt),
                recentAudit(2L, 11L, "모델B", "2.0", AuditStatus.COMPLIANT, completedAt.minusDays(1))
        );
        stubDashboard(summary(2, 1, 0, 1), List.of(), List.of(), List.of(), List.of(), recentAudits);

        AuditEntity audit1 = audit(1L);
        FairnessResultEntity failResult = FairnessResultEntity.of(audit1, "GENDER",
                FairnessMetricCode.DEMOGRAPHIC_PARITY, new BigDecimal("0.35"), new BigDecimal("0.20"),
                FairnessStatus.FAIL);
        given(dashboardQueryRepository.findFairnessResultsByAuditIds(List.of(1L, 2L)))
                .willReturn(List.of(failResult));
        given(dashboardQueryRepository.findXaiResultsByAuditIds(List.of(1L, 2L)))
                .willReturn(List.<XaiResultEntity>of());

        DashboardResponse result = dashboardService.getDashboard();

        assertThat(result.recentAudits()).hasSize(2);
        assertThat(result.recentAudits().get(0).auditId()).isEqualTo(1L);
        assertThat(result.recentAudits().get(0).keyRisk())
                .isEqualTo("GENDER DEMOGRAPHIC_PARITY 0.35 — 기준 0.2 초과");
        assertThat(result.recentAudits().get(1).keyRisk()).isEqualTo("분석 결과 없음");
    }

    @Test
    void 이상_신호가_없는_감사는_이상_신호_없음으로_표시된다() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        List<RecentAuditProjection> recentAudits =
                List.of(recentAudit(1L, 10L, "모델A", "1.0", AuditStatus.COMPLIANT, completedAt));
        stubDashboard(summary(1, 1, 0, 0), List.of(), List.of(), List.of(), List.of(), recentAudits);

        AuditEntity audit1 = audit(1L);
        FairnessResultEntity passResult = FairnessResultEntity.of(audit1, "GENDER",
                FairnessMetricCode.DEMOGRAPHIC_PARITY, new BigDecimal("0.10"), new BigDecimal("0.20"),
                FairnessStatus.PASS);
        given(dashboardQueryRepository.findFairnessResultsByAuditIds(List.of(1L)))
                .willReturn(List.of(passResult));
        given(dashboardQueryRepository.findXaiResultsByAuditIds(List.of(1L)))
                .willReturn(List.<XaiResultEntity>of());

        DashboardResponse result = dashboardService.getDashboard();

        assertThat(result.recentAudits().get(0).keyRisk()).isEqualTo("이상 신호 없음");
    }

    @Test
    void 분석_결과가_없는_감사는_분석_결과_없음으로_표시된다() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        List<RecentAuditProjection> recentAudits =
                List.of(recentAudit(1L, 10L, "모델A", "1.0", AuditStatus.COMPLIANT, completedAt));
        stubDashboard(summary(1, 1, 0, 0), List.of(), List.of(), List.of(), List.of(), recentAudits);

        given(dashboardQueryRepository.findFairnessResultsByAuditIds(List.of(1L)))
                .willReturn(List.<FairnessResultEntity>of());
        given(dashboardQueryRepository.findXaiResultsByAuditIds(List.of(1L)))
                .willReturn(List.<XaiResultEntity>of());

        DashboardResponse result = dashboardService.getDashboard();

        assertThat(result.recentAudits().get(0).keyRisk()).isEqualTo("분석 결과 없음");
    }
}
