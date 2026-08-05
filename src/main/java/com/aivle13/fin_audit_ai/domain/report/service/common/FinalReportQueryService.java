package com.aivle13.fin_audit_ai.domain.report.service.common;

import com.aivle13.fin_audit_ai.domain.report.dto.response.common.FinalReportMetadataResponse;
import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.exception.report.ReportNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이미 만들어 둔 최종 감사 보고서를 찾는다.
 *
 * <p>클라이언트가 다운로드 전에 이 조회를 먼저 하면, 있는 산출물을 다시 만들지 않아도 된다.
 * 아직 없으면 {@code REPORT_NOT_FOUND} 로 응답해 리포트 5종과 같은 방식으로 구분하게 한다.
 */
@Service
@RequiredArgsConstructor
public class FinalReportQueryService {

    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public FinalReportMetadataResponse getLatest(
            Long userId,
            Long auditId,
            ReportFormat format
    ) {
        ReportEntity report = reportRepository
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
                        auditId,
                        userId,
                        ReportType.FINAL_AUDIT_REPORT,
                        format
                )
                .orElseThrow(ReportNotFoundException::new);

        return FinalReportMetadataResponse.from(report);
    }
}
