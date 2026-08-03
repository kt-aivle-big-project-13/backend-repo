package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.s3.dto.DownloadedFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportDownloadServiceTest {

    private static final Long OWNER_ID = 2L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long REPORT_ID = 31L;
    private static final Long AUDIT_ID = 21L;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ReportEntity report;

    @Mock
    private AuditEntity audit;

    @Mock
    private DownloadedFile downloadedFile;

    @InjectMocks
    private ReportDownloadService service;

    @Test
    @DisplayName("소유자는 최종 감사 보고서를 내려받는다")
    void ownerCanDownload() {
        given(reportRepository.findByIdAndAudit_User_IdAndReportType(
                REPORT_ID,
                OWNER_ID,
                ReportType.FINAL_AUDIT_REPORT
        )).willReturn(Optional.of(report));

        given(report.getFilePath()).willReturn("reports/final.pdf");
        given(report.getFormat()).willReturn(ReportFormat.PDF);
        given(report.getAudit()).willReturn(audit);
        given(audit.getId()).willReturn(AUDIT_ID);
        given(fileStorageService.download(anyString())).willReturn(downloadedFile);

        ReportDownloadResult result = service.download(OWNER_ID, REPORT_ID);

        assertThat(result.fileName())
                .isEqualTo("final-audit-report-%d.pdf".formatted(AUDIT_ID));
    }

    @Test
    @DisplayName("다른 사용자의 보고서는 내려받을 수 없다")
    void rejectsDownloadOfAnotherUsersReport() {
        // 소유자로 좁힌 조회라 남의 보고서는 애초에 조회되지 않는다.
        given(reportRepository.findByIdAndAudit_User_IdAndReportType(
                REPORT_ID,
                OTHER_USER_ID,
                ReportType.FINAL_AUDIT_REPORT
        )).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(OTHER_USER_ID, REPORT_ID))
                .isInstanceOf(BusinessException.class);

        // 존재 여부가 드러나지 않도록 S3 접근까지 가지 않는다.
        verify(fileStorageService, never()).download(any());
    }
}
