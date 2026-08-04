package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.projection.ReportPreGenerationTargetProjection;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportPreGenerationServiceTest {

    private static final Long AUDIT_ID = 42L;
    private static final Long USER_ID = 7L;
    private static final Long ASSESSMENT_ID = 11L;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private ExplainabilityReportGenerationService explainabilityService;

    @Mock
    private BiasReportGenerationService biasService;

    @Mock
    private HighImpactReportGenerationService highImpactService;

    @Mock
    private ComplianceReportGenerationService complianceService;

    @Mock
    private ImprovementGuideGenerationService improvementService;

    @Mock
    private ReportGenerationService finalReportService;

    @Mock
    private ReportPreGenerationTargetProjection target;

    private ReportPreGenerationService service;

    @BeforeEach
    void setUp() {
        // 제출 즉시 같은 스레드에서 실행해 결과를 바로 검증한다.
        service = new ReportPreGenerationService(
                auditRepository,
                Runnable::run,
                explainabilityService,
                biasService,
                highImpactService,
                complianceService,
                improvementService,
                finalReportService
        );
    }

    @Test
    @DisplayName("분석 완료 후 설명가능성·편향진단·고영향 사전진단을 만든다")
    void generatesAnalysisReports() {
        givenTarget(ASSESSMENT_ID);

        service.preGenerateAfterAnalysis(AUDIT_ID);

        verify(explainabilityService).generateAndSave(USER_ID, AUDIT_ID);
        verify(biasService).generateAndSave(USER_ID, AUDIT_ID);
        verify(highImpactService).generateAndSave(USER_ID, AUDIT_ID);
    }

    @Test
    @DisplayName("사전진단을 건너뛴 감사에서는 고영향 사전진단을 만들지 않는다")
    void skipsHighImpactWhenPreDiagnosisSkipped() {
        givenTarget(null);

        service.preGenerateAfterAnalysis(AUDIT_ID);

        verify(explainabilityService).generateAndSave(USER_ID, AUDIT_ID);
        verify(biasService).generateAndSave(USER_ID, AUDIT_ID);
        verify(highImpactService, never()).generateAndSave(USER_ID, AUDIT_ID);
    }

    @Test
    @DisplayName("자율점검 이후에는 규제준수 판정서와 개선 권고 가이드를 만든다")
    void generatesSelfCheckReports() {
        // 자율점검 단계에서는 사전진단 연결 여부를 보지 않으므로 사용자만 준비한다.
        given(auditRepository.findReportPreGenerationTargetById(AUDIT_ID))
                .willReturn(Optional.of(target));
        given(target.getUserId()).willReturn(USER_ID);

        service.preGenerateAfterSelfCheck(AUDIT_ID);

        verify(complianceService).generateAndSave(USER_ID, AUDIT_ID);
        verify(improvementService).generateAndSave(USER_ID, AUDIT_ID);

        // 최종 보고서만 포맷별로 파일이 갈리므로 PDF·Word 를 함께 요청한다.
        verify(finalReportService).generate(
                USER_ID,
                AUDIT_ID,
                List.of(ReportFormat.PDF, ReportFormat.WORD)
        );

        // 분석 단계에서 이미 만든 리포트를 다시 만들지 않는다.
        verify(explainabilityService, never()).generateAndSave(USER_ID, AUDIT_ID);
        verify(biasService, never()).generateAndSave(USER_ID, AUDIT_ID);
    }

    @Test
    @DisplayName("한 리포트가 실패해도 나머지는 계속 만든다")
    void continuesWhenOneReportFails() {
        givenTarget(null);

        willThrow(new RuntimeException("AI 서버 장애"))
                .given(explainabilityService).generateAndSave(USER_ID, AUDIT_ID);

        assertThatCode(() -> service.preGenerateAfterAnalysis(AUDIT_ID))
                .doesNotThrowAnyException();

        verify(biasService).generateAndSave(USER_ID, AUDIT_ID);
    }

    @Test
    @DisplayName("감사 조회가 실패해도 호출부로 예외를 넘기지 않는다")
    void swallowsLookupFailure() {
        willThrow(new RuntimeException("DB 장애"))
                .given(auditRepository)
                .findReportPreGenerationTargetById(AUDIT_ID);

        // 여기서 예외가 나가면 성공한 감사가 실패로 뒤집힌다.
        assertThatCode(() -> service.preGenerateAfterAnalysis(AUDIT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("감사를 찾지 못하면 아무 리포트도 만들지 않는다")
    void generatesNothingWhenAuditMissing() {
        given(auditRepository.findReportPreGenerationTargetById(AUDIT_ID))
                .willReturn(Optional.empty());

        service.preGenerateAfterAnalysis(AUDIT_ID);

        verify(explainabilityService, never()).generateAndSave(USER_ID, AUDIT_ID);
        verify(biasService, never()).generateAndSave(USER_ID, AUDIT_ID);
    }

    private void givenTarget(Long assessmentId) {
        given(auditRepository.findReportPreGenerationTargetById(AUDIT_ID))
                .willReturn(Optional.of(target));
        given(target.getUserId()).willReturn(USER_ID);
        given(target.getAssessmentId()).willReturn(assessmentId);
    }
}
