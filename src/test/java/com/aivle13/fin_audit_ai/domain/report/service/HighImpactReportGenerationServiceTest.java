package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.diagnosis.entity.DiagnosisAnswerEntity;
import com.aivle13.fin_audit_ai.domain.diagnosis.entity.PreDiagnosisEntity;
import com.aivle13.fin_audit_ai.domain.diagnosis.repository.DiagnosisAnswerRepository;
import com.aivle13.fin_audit_ai.domain.diagnosis.repository.PreDiagnosisRepository;
import com.aivle13.fin_audit_ai.domain.diagnosis.type.DiagnosisResult;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.client.HighImpactReportClient;
import com.aivle13.fin_audit_ai.global.ai.dto.HighImpactReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.HighImpactReportResponse;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.diagnosis.PreDiagnosisNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HighImpactReportGenerationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long AUDIT_ID = 8L;
    private static final Long ASSESSMENT_ID = 50L;
    private static final Long PDF_REPORT_ID = 101L;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private PreDiagnosisRepository preDiagnosisRepository;

    @Mock
    private DiagnosisAnswerRepository diagnosisAnswerRepository;

    @Mock
    private HighImpactReportClient reportClient;

    @Mock
    private ReportPersistenceService reportPersistenceService;

    @Mock
    private AuditEntity audit;

    @Mock
    private AiModelEntity model;

    @Mock
    private PreDiagnosisEntity diagnosis;

    @InjectMocks
    private HighImpactReportGenerationService service;

    @Test
    void generatesAndSavesPdfAndWordReports() {
        givenHighImpactAssessment();

        given(reportClient.generate(
                any(HighImpactReportRequest.class)
        )).willReturn(validResponse());

        given(reportPersistenceService.saveAll(
                eq(AUDIT_ID),
                eq(ReportType.HIGH_IMPACT_REPORT),
                anyMap()
        )).willReturn(Map.of(
                ReportFormat.PDF,
                PDF_REPORT_ID,
                ReportFormat.WORD,
                102L
        ));

        Long result = service.generateAndSave(
                USER_ID,
                AUDIT_ID
        );

        assertThat(result).isEqualTo(PDF_REPORT_ID);

        ArgumentCaptor<HighImpactReportRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        HighImpactReportRequest.class
                );

        verify(reportClient).generate(
                requestCaptor.capture()
        );

        HighImpactReportRequest request =
                requestCaptor.getValue();

        assertThat(request.auditId()).isEqualTo(AUDIT_ID);
        assertThat(request.assessmentId())
                .isEqualTo(ASSESSMENT_ID);
        assertThat(request.auditName())
                .isEqualTo("신용평가 모델 감사");
        assertThat(request.modelName())
                .isEqualTo("신용평가 모델");
        assertThat(request.modelVersion())
                .isEqualTo("1.0");
        assertThat(request.assessedAt())
                .isEqualTo(
                        "2026-07-31T10:00:00+09:00"
                );
        assertThat(request.conditionMet()).isFalse();
        assertThat(request.groupAScore()).isEqualTo(4);
        assertThat(request.groupBScore()).isZero();
        assertThat(request.totalScore()).isEqualTo(4);
        assertThat(request.result())
                .isEqualTo("HIGH_IMPACT");

        assertThat(request.answers())
                .extracting(
                        HighImpactReportRequest.Answer::questionCode
                )
                .containsExactly(
                        "GATE_01",
                        "GATE_02",
                        "A_01",
                        "A_02",
                        "A_03",
                        "B_01",
                        "B_02",
                        "B_03"
                );

        HighImpactReportRequest.Answer firstAAnswer =
                request.answers().get(2);

        assertThat(firstAAnswer.questionText())
                .contains("복잡도");
        assertThat(firstAAnswer.stage())
                .isEqualTo("QUANTITATIVE");
        assertThat(firstAAnswer.group())
                .isEqualTo("A");
        assertThat(firstAAnswer.weight()).isEqualTo(2);
        assertThat(firstAAnswer.score()).isEqualTo(2);

        verify(reportPersistenceService).saveAll(
                AUDIT_ID,
                ReportType.HIGH_IMPACT_REPORT,
                Map.of(
                        ReportFormat.PDF,
                        validResponse().pdfReportS3Key(),
                        ReportFormat.WORD,
                        validResponse().wordReportS3Key()
                )
        );
    }

    @Test
    void rejectsAuditWithoutAssessmentId() {
        givenOwnedAudit();
        given(audit.getAssessmentId())
                .willReturn(null);

        assertThatThrownBy(() ->
                service.generateAndSave(
                        USER_ID,
                        AUDIT_ID
                )
        ).isInstanceOf(PreDiagnosisNotFoundException.class);

        verify(preDiagnosisRepository, never())
                .findByIdAndUser_Id(any(), any());
        verify(reportClient, never()).generate(any());
    }

    @Test
    void rejectsAssessmentNotOwnedByUser() {
        givenOwnedAudit();
        given(audit.getAssessmentId())
                .willReturn(ASSESSMENT_ID);
        given(preDiagnosisRepository.findByIdAndUser_Id(
                ASSESSMENT_ID,
                USER_ID
        )).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.generateAndSave(
                        USER_ID,
                        AUDIT_ID
                )
        ).isInstanceOf(PreDiagnosisNotFoundException.class);

        verify(reportClient, never()).generate(any());
    }

    @Test
    void rejectsNonHighImpactAssessment() {
        givenOwnedAudit();
        given(audit.getAssessmentId())
                .willReturn(ASSESSMENT_ID);
        given(preDiagnosisRepository.findByIdAndUser_Id(
                ASSESSMENT_ID,
                USER_ID
        )).willReturn(Optional.of(diagnosis));
        given(diagnosis.getResult())
                .willReturn(DiagnosisResult.NOT_APPLICABLE);

        assertThatThrownBy(() ->
                service.generateAndSave(
                        USER_ID,
                        AUDIT_ID
                )
        ).isInstanceOf(BusinessException.class);

        verify(reportClient, never()).generate(any());
    }

    @Test
    void rejectsInvalidAiServerResponse() {
        givenHighImpactAssessment();

        given(reportClient.generate(
                any(HighImpactReportRequest.class)
        )).willReturn(
                new HighImpactReportResponse(
                        AUDIT_ID,
                        999L,
                        "reports/report.pdf",
                        "reports/report.docx",
                        "2026-07-31T07:16:50Z"
                )
        );

        assertThatThrownBy(() ->
                service.generateAndSave(
                        USER_ID,
                        AUDIT_ID
                )
        ).isInstanceOf(AuditFailedException.class);

        verify(reportPersistenceService, never())
                .saveAll(any(), any(), anyMap());
    }

    @Test
    void rejectsStoredAnswerWithInvalidScore() {
        givenOwnedAudit();
        given(audit.getAssessmentId())
                .willReturn(ASSESSMENT_ID);
        given(preDiagnosisRepository.findByIdAndUser_Id(
                ASSESSMENT_ID,
                USER_ID
        )).willReturn(Optional.of(diagnosis));
        given(diagnosis.getResult())
                .willReturn(DiagnosisResult.HIGH_IMPACT);

        DiagnosisAnswerEntity invalidAnswer =
                DiagnosisAnswerEntity.of(
                        diagnosis,
                        "A_01",
                        true,
                        0
                );

        given(diagnosisAnswerRepository
                .findAllByDiagnosis_IdOrderByIdAsc(
                        ASSESSMENT_ID
                ))
                .willReturn(List.of(invalidAnswer));

        assertThatThrownBy(() ->
                service.generateAndSave(
                        USER_ID,
                        AUDIT_ID
                )
        ).isInstanceOf(AuditFailedException.class);

        verify(reportClient, never()).generate(any());
    }

    @Test
    void rejectsDiagnosisWithoutUpdatedAt() {
        givenOwnedAudit();

        given(audit.getAssessmentId())
                .willReturn(ASSESSMENT_ID);

        given(preDiagnosisRepository.findByIdAndUser_Id(
                ASSESSMENT_ID,
                USER_ID
        )).willReturn(Optional.of(diagnosis));

        given(diagnosis.getResult())
                .willReturn(DiagnosisResult.HIGH_IMPACT);

        given(diagnosisAnswerRepository
                .findAllByDiagnosis_IdOrderByIdAsc(
                        ASSESSMENT_ID
                ))
                .willReturn(
                        List.of(
                                answer(
                                        "GATE_01",
                                        true,
                                        0
                                )
                        )
                );

        assertThatThrownBy(() ->
                service.generateAndSave(
                        USER_ID,
                        AUDIT_ID
                )
        ).isInstanceOf(AuditFailedException.class);

        verify(reportClient, never())
                .generate(any());
    }

    private void givenHighImpactAssessment() {
        givenOwnedAudit();

        given(audit.getId())
                .willReturn(AUDIT_ID);

        given(audit.getAssessmentId())
                .willReturn(ASSESSMENT_ID);
        given(audit.getAuditName())
                .willReturn("신용평가 모델 감사");
        given(audit.getModel())
                .willReturn(model);
        given(model.getModelName())
                .willReturn("신용평가 모델");
        given(model.getVersion())
                .willReturn("1.0");

        given(preDiagnosisRepository.findByIdAndUser_Id(
                ASSESSMENT_ID,
                USER_ID
        )).willReturn(Optional.of(diagnosis));

        given(diagnosis.getId())
                .willReturn(ASSESSMENT_ID);
        given(diagnosis.getResult())
                .willReturn(DiagnosisResult.HIGH_IMPACT);
        given(diagnosis.getUpdatedAt())
                .willReturn(
                        LocalDateTime.of(
                                2026,
                                7,
                                31,
                                10,
                                0
                        )
                );
        given(diagnosis.isConditionMet())
                .willReturn(false);
        given(diagnosis.getGroupAScore())
                .willReturn(4);
        given(diagnosis.getGroupBScore())
                .willReturn(0);
        given(diagnosis.getTotalScore())
                .willReturn(4);

        given(diagnosisAnswerRepository
                .findAllByDiagnosis_IdOrderByIdAsc(
                        ASSESSMENT_ID
                ))
                .willReturn(quantitativeAnswers());
    }

    private void givenOwnedAudit() {
        given(auditRepository
                .findByIdAndUser_IdWithModelAndDataset(
                        AUDIT_ID,
                        USER_ID
                ))
                .willReturn(Optional.of(audit));
    }

    private List<DiagnosisAnswerEntity> quantitativeAnswers() {
        return List.of(
                answer("GATE_01", false, 0),
                answer("GATE_02", false, 0),
                answer("A_01", true, 2),
                answer("A_02", true, 2),
                answer("A_03", false, 0),
                answer("B_01", false, 0),
                answer("B_02", false, 0),
                answer("B_03", false, 0)
        );
    }

    private DiagnosisAnswerEntity answer(
            String questionCode,
            boolean selected,
            int score
    ) {
        return DiagnosisAnswerEntity.of(
                diagnosis,
                questionCode,
                selected,
                score
        );
    }

    private HighImpactReportResponse validResponse() {
        return new HighImpactReportResponse(
                AUDIT_ID,
                ASSESSMENT_ID,
                "high-impact-reports/8/50/run/report.pdf",
                "high-impact-reports/8/50/run/report.docx",
                "2026-07-31T07:16:50Z"
        );
    }
}
