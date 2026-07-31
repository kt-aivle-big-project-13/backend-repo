package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.SelfCheckItemCode;
import com.aivle13.fin_audit_ai.domain.audit.type.ThresholdMethod;
import com.aivle13.fin_audit_ai.domain.law.dto.AuditRegulationComplianceView;
import com.aivle13.fin_audit_ai.domain.law.service.AuditRegulationMappingQueryService;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.client.ComplianceReportClient;
import com.aivle13.fin_audit_ai.global.ai.dto.ComplianceReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.ComplianceReportResponse;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ComplianceReportGenerationServiceTest {

    private static final Long USER_ID = 2L;
    private static final Long AUDIT_ID = 55L;
    private static final Long REPORT_ID = 61L;
    private static final Long PDF_REPORT_ID = 62L;
    private static final Long WORD_REPORT_ID = 63L;

    private static final String HTML_S3_KEY =
            "compliance-reports/55/run-1/report.html";
    private static final String PDF_S3_KEY =
            "compliance-reports/55/run-1/report.pdf";
    private static final String WORD_S3_KEY =
            "compliance-reports/55/run-1/report.docx";

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private SelfCheckAnswerRepository selfCheckAnswerRepository;

    @Mock
    private FairnessResultRepository fairnessResultRepository;

    @Mock
    private AuditRegulationMappingQueryService regulationMappingQueryService;

    @Mock
    private ComplianceReportClient reportClient;

    @Mock
    private ReportPersistenceService reportPersistenceService;

    @Mock
    private AuditEntity audit;

    @Mock
    private AiModelEntity model;

    @InjectMocks
    private ComplianceReportGenerationService service;

    @Test
    void assemblesSelfCheckAnswersAndRegulationMappings() {
        givenAudit();
        givenSelfCheckAnswers();
        givenRegulationMappings();
        givenFairnessResults();
        givenSuccessfulGeneration();

        service.generateAndSave(USER_ID, AUDIT_ID);

        ComplianceReportRequest request = capturedRequest();

        assertThat(request.auditId()).isEqualTo(AUDIT_ID);
        assertThat(request.auditName()).isEqualTo("테스트 감사");
        assertThat(request.modelName()).isEqualTo("credit_model");

        assertThat(request.selfCheckAnswers())
                .extracting(
                        ComplianceReportRequest.SelfCheckAnswer::itemCode,
                        ComplianceReportRequest.SelfCheckAnswer::label,
                        ComplianceReportRequest.SelfCheckAnswer::answer
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "NOTICE",
                                SelfCheckItemCode.NOTICE.label(),
                                true
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                "OBJECTION",
                                SelfCheckItemCode.OBJECTION.label(),
                                false
                        )
                );

        // AuditRegulationComplianceView 의 articleTitle 이 법령명으로 넘어가야 한다.
        assertThat(request.regulationMappings())
                .extracting(
                        ComplianceReportRequest.RegulationMapping::lawName,
                        ComplianceReportRequest.RegulationMapping::articleNo,
                        ComplianceReportRequest.RegulationMapping::compliance,
                        ComplianceReportRequest.RegulationMapping::summary
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "신용정보법",
                                "제36조의2",
                                "NON_COMPLIANT",
                                "자동화 평가 결과 설명·이의제기 보장"
                        )
                );
    }

    @Test
    void includesFairnessValuesAsReferenceOnly() {
        givenAudit();
        givenSelfCheckAnswers();
        givenRegulationMappings();
        givenFairnessResults();
        givenSuccessfulGeneration();

        service.generateAndSave(USER_ID, AUDIT_ID);

        ComplianceReportRequest.AuditReference reference =
                capturedRequest().auditReference();

        assertThat(reference.thresholdMethod())
                .isEqualTo(ThresholdMethod.MANUAL.name());
        assertThat(reference.auc())
                .isEqualByComparingTo(new BigDecimal("0.7550"));

        assertThat(reference.fairness()).hasSize(1);
        assertThat(reference.fairness().get(0).attribute())
                .isEqualTo("CODE_GENDER");
        assertThat(reference.fairness().get(0).demographicParityDifference())
                .isEqualByComparingTo(new BigDecimal("0.0800"));
    }

    @Test
    void savesThreeFormatsInOneTransaction() {
        givenAudit();
        givenSelfCheckAnswers();
        givenRegulationMappings();
        givenFairnessResults();
        givenSuccessfulGeneration();

        Long result = service.generateAndSave(USER_ID, AUDIT_ID);

        assertThat(result).isEqualTo(REPORT_ID);

        verify(reportPersistenceService).saveAll(
                AUDIT_ID,
                ReportType.COMPLIANCE_VERDICT,
                Map.of(
                        ReportFormat.HTML, HTML_S3_KEY,
                        ReportFormat.PDF, PDF_S3_KEY,
                        ReportFormat.WORD, WORD_S3_KEY
                )
        );
    }

    @Test
    void rejectsWhenSelfCheckNotAnswered() {
        givenAuditLookup();

        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of());

        assertThatThrownBy(() ->
                service.generateAndSave(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditFailedException.class);

        verify(reportClient, never()).generate(any());
        verify(reportPersistenceService, never())
                .saveAll(any(), any(), any());
    }

    @Test
    void rejectsWhenRegulationMappingMissing() {
        givenAuditLookup();
        givenSelfCheckAnswers();

        given(regulationMappingQueryService.getMappings(AUDIT_ID))
                .willReturn(List.of());

        assertThatThrownBy(() ->
                service.generateAndSave(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditFailedException.class);

        verify(reportClient, never()).generate(any());
    }

    @Test
    void throwsWhenAuditDoesNotExist() {
        given(auditRepository.findByIdAndUser_IdWithModelAndDataset(
                AUDIT_ID,
                USER_ID
        ))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.generateAndSave(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditNotFoundException.class);

        verify(reportClient, never()).generate(any());
    }

    @Test
    void rejectsResponseWithoutWordKey() {
        givenAudit();
        givenSelfCheckAnswers();
        givenRegulationMappings();
        givenFairnessResults();

        given(reportClient.generate(any(ComplianceReportRequest.class)))
                .willReturn(new ComplianceReportResponse(
                        AUDIT_ID,
                        HTML_S3_KEY,
                        PDF_S3_KEY,
                        "",
                        "html",
                        1,
                        1,
                        0,
                        "2026-07-31T10:00:00Z"
                ));

        assertThatThrownBy(() ->
                service.generateAndSave(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditFailedException.class);

        verify(reportPersistenceService, never())
                .saveAll(any(), any(), any());
    }

    private ComplianceReportRequest capturedRequest() {
        ArgumentCaptor<ComplianceReportRequest> captor =
                ArgumentCaptor.forClass(ComplianceReportRequest.class);

        verify(reportClient).generate(captor.capture());

        return captor.getValue();
    }

    // 감사 조회까지만 필요한 셋업. 그 뒤 단계에서 실패하는 테스트가 쓴다.
    private void givenAuditLookup() {
        given(auditRepository.findByIdAndUser_IdWithModelAndDataset(
                AUDIT_ID,
                USER_ID
        ))
                .willReturn(Optional.of(audit));
    }

    private void givenAudit() {
        givenAuditLookup();

        given(audit.getId()).willReturn(AUDIT_ID);
        given(audit.getAuditName()).willReturn("테스트 감사");
        given(audit.getModel()).willReturn(model);
        given(model.getModelName()).willReturn("credit_model");
    }

    private void givenSelfCheckAnswers() {
        // 스터빙 안에서 다시 스터빙하면 UnfinishedStubbingException 이 나므로
        // mock 을 먼저 완성한 뒤 넘긴다.
        SelfCheckAnswerEntity notice = answer(SelfCheckItemCode.NOTICE, true);
        SelfCheckAnswerEntity objection = answer(SelfCheckItemCode.OBJECTION, false);

        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(notice, objection));
    }

    private void givenRegulationMappings() {
        given(regulationMappingQueryService.getMappings(AUDIT_ID))
                .willReturn(List.of(new AuditRegulationComplianceView(
                        1L,
                        "제36조의2",
                        "신용정보법",
                        "조항 본문",
                        "자동화 평가 결과 설명·이의제기 보장",
                        ComplianceStatus.NON_COMPLIANT,
                        "자율점검 기반 자동 매칭",
                        List.of()
                )));
    }

    private void givenFairnessResults() {
        given(audit.getThresholdMethod())
                .willReturn(ThresholdMethod.MANUAL);
        given(audit.getManualThreshold())
                .willReturn(new BigDecimal("0.5000"));
        given(audit.getModelAuc())
                .willReturn(new BigDecimal("0.7550"));
        given(audit.getModelAccuracy())
                .willReturn(new BigDecimal("0.7100"));

        FairnessResultEntity result = fairnessResult(
                FairnessMetricCode.DEMOGRAPHIC_PARITY,
                new BigDecimal("0.0800")
        );

        given(fairnessResultRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(result));
    }

    private void givenSuccessfulGeneration() {
        given(reportClient.generate(any(ComplianceReportRequest.class)))
                .willReturn(new ComplianceReportResponse(
                        AUDIT_ID,
                        HTML_S3_KEY,
                        PDF_S3_KEY,
                        WORD_S3_KEY,
                        "html",
                        1,
                        1,
                        0,
                        "2026-07-31T10:00:00Z"
                ));

        given(reportPersistenceService.saveAll(
                AUDIT_ID,
                ReportType.COMPLIANCE_VERDICT,
                Map.of(
                        ReportFormat.HTML, HTML_S3_KEY,
                        ReportFormat.PDF, PDF_S3_KEY,
                        ReportFormat.WORD, WORD_S3_KEY
                )
        )).willReturn(Map.of(
                ReportFormat.HTML, REPORT_ID,
                ReportFormat.PDF, PDF_REPORT_ID,
                ReportFormat.WORD, WORD_REPORT_ID
        ));
    }

    private SelfCheckAnswerEntity answer(
            SelfCheckItemCode itemCode,
            boolean value
    ) {
        SelfCheckAnswerEntity entity =
                org.mockito.Mockito.mock(SelfCheckAnswerEntity.class);

        given(entity.getItemCode()).willReturn(itemCode);
        given(entity.isAnswer()).willReturn(value);

        return entity;
    }

    private FairnessResultEntity fairnessResult(
            FairnessMetricCode metricCode,
            BigDecimal value
    ) {
        FairnessResultEntity entity =
                org.mockito.Mockito.mock(FairnessResultEntity.class);

        given(entity.getAttribute()).willReturn("CODE_GENDER");
        given(entity.getMetricCode()).willReturn(metricCode);
        given(entity.getValue()).willReturn(value);

        return entity;
    }
}
