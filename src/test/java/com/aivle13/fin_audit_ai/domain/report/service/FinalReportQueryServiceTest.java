package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.report.dto.FinalReportMetadataResponse;
import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportStatus;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.exception.report.ReportNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FinalReportQueryServiceTest {

    private static final Long OWNER_ID = 2L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long AUDIT_ID = 21L;
    private static final Long REPORT_ID = 31L;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportEntity report;

    @Mock
    private AuditEntity audit;

    @InjectMocks
    private FinalReportQueryService service;

    @Test
    @DisplayName("이미 만들어 둔 최종 보고서를 포맷별로 찾는다")
    void returnsLatestReport() {
        given(reportRepository
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
                        AUDIT_ID,
                        OWNER_ID,
                        ReportType.FINAL_AUDIT_REPORT,
                        ReportFormat.PDF
                )).willReturn(Optional.of(report));

        given(report.getId()).willReturn(REPORT_ID);
        given(report.getAudit()).willReturn(audit);
        given(audit.getId()).willReturn(AUDIT_ID);
        given(report.getReportType()).willReturn(ReportType.FINAL_AUDIT_REPORT);
        given(report.getFormat()).willReturn(ReportFormat.PDF);
        given(report.getStatus()).willReturn(ReportStatus.COMPLETED);
        given(report.getVersion()).willReturn(1);

        FinalReportMetadataResponse response =
                service.getLatest(OWNER_ID, AUDIT_ID, ReportFormat.PDF);

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        assertThat(response.format()).isEqualTo(ReportFormat.PDF);
        assertThat(response.status()).isEqualTo(ReportStatus.COMPLETED);
    }

    @Test
    @DisplayName("아직 만들어지지 않았으면 찾을 수 없음으로 응답한다")
    void throwsWhenNotGeneratedYet() {
        given(reportRepository
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
                        AUDIT_ID,
                        OWNER_ID,
                        ReportType.FINAL_AUDIT_REPORT,
                        ReportFormat.WORD
                )).willReturn(Optional.empty());

        // 클라이언트는 이 응답을 보고 생성 요청으로 넘어간다.
        assertThatThrownBy(() ->
                service.getLatest(OWNER_ID, AUDIT_ID, ReportFormat.WORD)
        ).isInstanceOf(ReportNotFoundException.class);
    }

    @Test
    @DisplayName("다른 사용자의 감사는 조회되지 않는다")
    void doesNotReturnAnotherUsersReport() {
        given(reportRepository
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
                        AUDIT_ID,
                        OTHER_USER_ID,
                        ReportType.FINAL_AUDIT_REPORT,
                        ReportFormat.PDF
                )).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getLatest(OTHER_USER_ID, AUDIT_ID, ReportFormat.PDF)
        ).isInstanceOf(ReportNotFoundException.class);
    }
}
