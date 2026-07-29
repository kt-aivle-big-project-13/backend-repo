package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportStatus;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportPersistenceServiceTest {

    private static final Long AUDIT_ID = 21L;
    private static final Long REPORT_ID = 31L;
    private static final String S3_KEY =
            "explainability-reports/21/run-123/report.html";

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AuditEntity audit;

    @Mock
    private ReportEntity savedReport;

    @InjectMocks
    private ReportPersistenceService service;

    @Test
    void savesExplainabilityHtmlReport() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));

        given(reportRepository.save(any(ReportEntity.class)))
                .willReturn(savedReport);

        given(savedReport.getId()).willReturn(REPORT_ID);

        Long result = service.save(
                AUDIT_ID,
                ReportType.XAI_REPORT,
                ReportFormat.HTML,
                S3_KEY
        );

        assertThat(result).isEqualTo(REPORT_ID);

        ArgumentCaptor<ReportEntity> reportCaptor =
                ArgumentCaptor.forClass(ReportEntity.class);

        verify(reportRepository).save(reportCaptor.capture());

        ReportEntity report = reportCaptor.getValue();

        assertThat(report.getAudit()).isSameAs(audit);
        assertThat(report.getReportType())
                .isEqualTo(ReportType.XAI_REPORT);
        assertThat(report.getFormat())
                .isEqualTo(ReportFormat.HTML);
        assertThat(report.getStatus())
                .isEqualTo(ReportStatus.COMPLETED);
        assertThat(report.getFilePath())
                .isEqualTo(S3_KEY);

        verify(fileStorageService)
                .deleteOnRollback(List.of(S3_KEY));
    }
}
