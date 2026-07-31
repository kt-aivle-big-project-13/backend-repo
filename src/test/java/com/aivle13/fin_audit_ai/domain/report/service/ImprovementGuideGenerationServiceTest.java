package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.client.ImprovementGuideClient;
import com.aivle13.fin_audit_ai.global.ai.dto.ImprovementGuideRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.ImprovementGuideResponse;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ImprovementGuideGenerationServiceTest {

    private static final Long USER_ID = 2L;
    private static final Long AUDIT_ID = 77L;
    private static final Long REPORT_ID = 61L;
    private static final Long PDF_REPORT_ID = 62L;
    private static final Long WORD_REPORT_ID = 63L;

    private static final String HTML_S3_KEY =
            "improvement-guides/77/run-1/report.html";
    private static final String PDF_S3_KEY =
            "improvement-guides/77/run-1/report.pdf";
    private static final String WORD_S3_KEY =
            "improvement-guides/77/run-1/report.docx";

    @Mock
    private ImprovementGuideRequestAssembler requestAssembler;

    @Mock
    private ImprovementGuideClient reportClient;

    @Mock
    private ReportPersistenceService reportPersistenceService;

    @InjectMocks
    private ImprovementGuideGenerationService service;

    @Test
    void savesThreeFormatsInOneTransaction() {
        givenAssembledRequest();
        givenSuccessfulGeneration();

        Long result = service.generateAndSave(USER_ID, AUDIT_ID);

        assertThat(result).isEqualTo(REPORT_ID);

        verify(reportPersistenceService).saveAll(
                AUDIT_ID,
                ReportType.IMPROVEMENT_GUIDE,
                Map.of(
                        ReportFormat.HTML, HTML_S3_KEY,
                        ReportFormat.PDF, PDF_S3_KEY,
                        ReportFormat.WORD, WORD_S3_KEY
                )
        );
    }

    @Test
    void passesAssembledRequestToAiServer() {
        ImprovementGuideRequest request = givenAssembledRequest();
        givenSuccessfulGeneration();

        service.generateAndSave(USER_ID, AUDIT_ID);

        // 조립은 별도 빈이 트랜잭션 안에서 수행하고, 이 서비스는 그 결과를 그대로 넘긴다.
        verify(reportClient).generate(request);
    }

    @Test
    void propagatesAssemblyFailureWithoutCallingAiServer() {
        willThrow(new AuditNotFoundException())
                .given(requestAssembler)
                .assemble(USER_ID, AUDIT_ID);

        assertThatThrownBy(() ->
                service.generateAndSave(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditNotFoundException.class);

        verify(reportClient, never()).generate(any());
        verify(reportPersistenceService, never())
                .saveAll(any(), any(), any());
    }

    @Test
    void rejectsResponseWithoutWordKey() {
        givenAssembledRequest();

        given(reportClient.generate(any(ImprovementGuideRequest.class)))
                .willReturn(response(""));

        assertThatThrownBy(() ->
                service.generateAndSave(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditFailedException.class);

        verify(reportPersistenceService, never())
                .saveAll(any(), any(), any());
    }

    @Test
    void rejectsResponseWithMismatchedAuditId() {
        givenAssembledRequest();

        given(reportClient.generate(any(ImprovementGuideRequest.class)))
                .willReturn(new ImprovementGuideResponse(
                        999L,
                        HTML_S3_KEY,
                        PDF_S3_KEY,
                        WORD_S3_KEY,
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

    private ImprovementGuideRequest givenAssembledRequest() {
        ImprovementGuideRequest request = new ImprovementGuideRequest(
                AUDIT_ID,
                "테스트 감사",
                "credit_model",
                List.of(new ImprovementGuideRequest.ComplianceGap(
                        "신용정보법",
                        "제36조의2",
                        "요지",
                        "자율점검 기반 자동 매칭"
                )),
                List.of(new ImprovementGuideRequest.SelfCheckGap(
                        "OBJECTION",
                        "심사 결과 이의제기 절차 마련 여부"
                )),
                List.of(new ImprovementGuideRequest.MetricFinding(
                        "CODE_GENDER",
                        "DEMOGRAPHIC_PARITY",
                        new BigDecimal("0.4500"),
                        new BigDecimal("0.2000"),
                        "FAIL"
                )),
                List.of()
        );

        given(requestAssembler.assemble(USER_ID, AUDIT_ID))
                .willReturn(request);

        return request;
    }

    private void givenSuccessfulGeneration() {
        given(reportClient.generate(any(ImprovementGuideRequest.class)))
                .willReturn(response(WORD_S3_KEY));

        given(reportPersistenceService.saveAll(
                AUDIT_ID,
                ReportType.IMPROVEMENT_GUIDE,
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

    private ImprovementGuideResponse response(String wordKey) {
        return new ImprovementGuideResponse(
                AUDIT_ID,
                HTML_S3_KEY,
                PDF_S3_KEY,
                wordKey,
                "html",
                1,
                1,
                0,
                "2026-07-31T10:00:00Z"
        );
    }
}
