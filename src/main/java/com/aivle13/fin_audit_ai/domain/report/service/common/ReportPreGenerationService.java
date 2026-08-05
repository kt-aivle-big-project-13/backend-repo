package com.aivle13.fin_audit_ai.domain.report.service.common;

import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.projection.ReportPreGenerationTargetProjection;
import com.aivle13.fin_audit_ai.domain.report.service.bias.BiasReportGenerationService;
import com.aivle13.fin_audit_ai.domain.report.service.compliance.ComplianceReportGenerationService;
import com.aivle13.fin_audit_ai.domain.report.service.explainability.ExplainabilityReportGenerationService;
import com.aivle13.fin_audit_ai.domain.report.service.highimpact.HighImpactReportGenerationService;
import com.aivle13.fin_audit_ai.domain.report.service.improvement.ImprovementGuideGenerationService;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * 자동 생성 보고서를 사용자가 요청하기 전에 미리 만들어 둔다.
 *
 * <p>생성은 AI 서버 호출과 PDF 렌더를 포함해 건당 수십 초가 걸린다. 다운로드를 누른 뒤에
 * 만들면 그 시간을 사용자가 그대로 기다리게 되므로, 재료가 갖춰지는 시점에 미리 만든다.
 *
 * <p>재료가 갖춰지는 시점이 리포트마다 다르다. 설명가능성·편향진단·고영향 사전진단은 분석이
 * 끝나면 만들 수 있지만, 규제준수 판정서와 개선 권고 가이드는 자율점검 응답과 법령 매핑이
 * 있어야 한다.
 *
 * <p>선생성은 부수 작업이다. 실패해도 감사나 자율점검 흐름을 실패시키지 않는다. 미리 만들지
 * 못했더라도 사용자가 다운로드할 때 기존 경로로 생성되므로 기능 자체는 유지된다.
 *
 * <p>{@code @Async} 대신 실행기를 직접 받아 쓴다. 종류별로 각각 제출해야 병렬로 만들어지는데,
 * 같은 클래스 안에서 {@code @Async} 메서드를 호출하면 프록시를 거치지 않아 순차 실행된다.
 */
@Slf4j
@Service
public class ReportPreGenerationService {

    private final AuditRepository auditRepository;
    private final Executor reportTaskExecutor;
    private final ExplainabilityReportGenerationService explainabilityService;
    private final BiasReportGenerationService biasService;
    private final HighImpactReportGenerationService highImpactService;
    private final ComplianceReportGenerationService complianceService;
    private final ImprovementGuideGenerationService improvementService;
    private final ReportGenerationService finalReportService;

    public ReportPreGenerationService(
            AuditRepository auditRepository,
            @Qualifier("reportTaskExecutor") Executor reportTaskExecutor,
            ExplainabilityReportGenerationService explainabilityService,
            BiasReportGenerationService biasService,
            HighImpactReportGenerationService highImpactService,
            ComplianceReportGenerationService complianceService,
            ImprovementGuideGenerationService improvementService,
            ReportGenerationService finalReportService
    ) {
        this.auditRepository = auditRepository;
        this.reportTaskExecutor = reportTaskExecutor;
        this.explainabilityService = explainabilityService;
        this.biasService = biasService;
        this.highImpactService = highImpactService;
        this.complianceService = complianceService;
        this.improvementService = improvementService;
        this.finalReportService = finalReportService;
    }

    /** 분석이 끝난 직후 만들 수 있는 리포트를 병렬로 제출한다. */
    public void preGenerateAfterAnalysis(Long auditId) {
        submitSafely(auditId, this::submitAnalysisReports);
    }

    /** 자율점검 응답과 법령 매핑이 갖춰진 뒤에야 만들 수 있는 리포트를 병렬로 제출한다. */
    public void preGenerateAfterSelfCheck(Long auditId) {
        submitSafely(auditId, this::submitSelfCheckReports);
    }

    /**
     * 제출 과정에서 난 예외를 호출부로 넘기지 않는다.
     *
     * <p>호출부는 감사 분석과 자율점검 매핑 리스너인데, 둘 다 예외가 오면 감사를 실패로
     * 표시한다. 선생성은 부수 작업이라 그것 때문에 성공한 감사가 실패로 뒤집히면 안 된다.
     */
    private void submitSafely(Long auditId, Consumer<Long> submit) {
        try {
            submit.accept(auditId);
        } catch (RuntimeException exception) {
            log.warn(
                    "보고서 선생성을 시작하지 못했습니다. 다운로드 시점에 생성됩니다: auditId={}",
                    auditId,
                    exception
            );
        }
    }

    private void submitAnalysisReports(Long auditId) {
        findTarget(auditId).ifPresent(target -> {
            List<ReportType> reportTypes = new ArrayList<>(
                    List.of(ReportType.XAI_REPORT, ReportType.BIAS_REPORT)
            );

            // 사전진단을 건너뛴 감사는 연결된 사전진단이 없어 보고서를 만들 수 없다.
            if (target.getAssessmentId() != null) {
                reportTypes.add(ReportType.HIGH_IMPACT_REPORT);
            }

            submitAll(target.getUserId(), auditId, reportTypes);
        });
    }

    private void submitSelfCheckReports(Long auditId) {
        findTarget(auditId).ifPresent(target ->
                submitAll(
                        target.getUserId(),
                        auditId,
                        List.of(
                                ReportType.COMPLIANCE_VERDICT,
                                ReportType.IMPROVEMENT_GUIDE,
                                ReportType.FINAL_AUDIT_REPORT
                        )
                )
        );
    }

    private void submitAll(
            Long userId,
            Long auditId,
            List<ReportType> reportTypes
    ) {
        for (ReportType reportType : reportTypes) {
            reportTaskExecutor.execute(() ->
                    generateQuietly(userId, auditId, reportType)
            );
        }
    }

    /**
     * 한 종류를 만든다. 하나가 실패해도 다른 종류의 생성에는 영향이 없다.
     *
     * <p>선생성에서 실패한 리포트는 사용자가 다운로드할 때 다시 시도되므로 여기서 재시도하지
     * 않는다. 예외를 밖으로 던지면 실행기 스레드에서 그대로 죽으므로 여기서 잡는다.
     */
    private void generateQuietly(
            Long userId,
            Long auditId,
            ReportType reportType
    ) {
        try {
            generate(userId, auditId, reportType);

            log.info(
                    "보고서 선생성 완료: auditId={}, reportType={}",
                    auditId,
                    reportType
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "보고서 선생성 실패, 다운로드 시점에 다시 생성됩니다: "
                            + "auditId={}, reportType={}",
                    auditId,
                    reportType,
                    exception
            );
        }
    }

    private void generate(
            Long userId,
            Long auditId,
            ReportType reportType
    ) {
        switch (reportType) {
            case XAI_REPORT ->
                    explainabilityService.generateAndSave(userId, auditId);
            case BIAS_REPORT ->
                    biasService.generateAndSave(userId, auditId);
            case HIGH_IMPACT_REPORT ->
                    highImpactService.generateAndSave(userId, auditId);
            case COMPLIANCE_VERDICT ->
                    complianceService.generateAndSave(userId, auditId);
            case IMPROVEMENT_GUIDE ->
                    improvementService.generateAndSave(userId, auditId);
            // 최종 보고서만 포맷별로 파일이 갈린다. 한 번 호출로 PDF·Word 를 함께 만든다.
            case FINAL_AUDIT_REPORT ->
                    finalReportService.generate(
                            userId,
                            auditId,
                            List.of(ReportFormat.PDF, ReportFormat.WORD)
                    );
            default -> log.warn(
                    "선생성 대상이 아닌 리포트 종류입니다: reportType={}",
                    reportType
            );
        }
    }

    private Optional<ReportPreGenerationTargetProjection> findTarget(Long auditId) {
        Optional<ReportPreGenerationTargetProjection> target =
                auditRepository.findReportPreGenerationTargetById(auditId);

        if (target.isEmpty()) {
            log.warn(
                    "보고서를 선생성할 감사를 찾지 못했습니다: auditId={}",
                    auditId
            );
        }

        return target;
    }
}
