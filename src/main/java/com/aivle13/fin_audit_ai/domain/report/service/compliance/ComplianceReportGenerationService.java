package com.aivle13.fin_audit_ai.domain.report.service.compliance;

import com.aivle13.fin_audit_ai.domain.report.service.common.ReportNarrativeRecorder;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportPersistenceService;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.client.report.ComplianceReportClient;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.ComplianceReportResponse;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 규제준수 판정서 생성.
 *
 * <p>판정은 여기서 내리지 않는다. 조항별 준수/미준수는 {@code AuditRegulationMappingService}
 * 가 자가점검 응답으로 이미 정해 저장해 뒀고, {@link ComplianceReportRequestAssembler} 가
 * 그 결과를 한 트랜잭션에서 조립한다. 이 서비스는 AI 호출과 산출물 저장만 담당한다.
 */
@Service
@RequiredArgsConstructor
public class ComplianceReportGenerationService {

    private final ComplianceReportRequestAssembler requestAssembler;
    private final ComplianceReportClient reportClient;
    private final ReportPersistenceService reportPersistenceService;
    private final ReportNarrativeRecorder narrativeRecorder;

    public Long generateAndSave(
            Long userId,
            Long auditId
    ) {
        // 조립은 짧은 트랜잭션으로 끝내고, 수십 초 걸리는 AI 호출은 트랜잭션 밖에서 한다.
        ComplianceReportResponse response =
                reportClient.generate(
                        requestAssembler.assemble(userId, auditId)
                );

        validateResponse(auditId, response);

        // HTML·PDF·Word를 한 트랜잭션으로 저장한다. 하나라도 실패하면 업로드된
        // S3 파일까지 함께 정리된다.
        Map<ReportFormat, Long> reportIds =
                reportPersistenceService.saveAll(
                        auditId,
                        ReportType.COMPLIANCE_VERDICT,
                        Map.of(
                                ReportFormat.HTML,
                                response.reportS3Key(),
                                ReportFormat.PDF,
                                response.pdfReportS3Key(),
                                ReportFormat.WORD,
                                response.wordReportS3Key()
                        )
                );

        // 생성 응답은 다른 리포트와 같이 HTML 리포트 ID를 반환한다.
        Long htmlReportId = reportIds.get(ReportFormat.HTML);

        if (htmlReportId == null) {
            throw new AuditFailedException();
        }

        // 리포트 저장이 끝난 뒤에 서술을 남긴다. 먼저 저장하면 리포트 저장이 실패했을 때
        // 존재하지 않는 리포트의 서술을 챗봇이 근거로 인용하게 된다.
        narrativeRecorder.record(
                auditId,
                ReportType.COMPLIANCE_VERDICT,
                response.narratives()
        );

        return htmlReportId;
    }

    private void validateResponse(
            Long auditId,
            ComplianceReportResponse response
    ) {
        if (response == null
                || !auditId.equals(response.auditId())
                || isBlank(response.reportS3Key())
                || isBlank(response.pdfReportS3Key())
                || isBlank(response.wordReportS3Key())
                || response.format() == null
                || !"html".equalsIgnoreCase(response.format().trim())) {
            throw new AuditFailedException();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
