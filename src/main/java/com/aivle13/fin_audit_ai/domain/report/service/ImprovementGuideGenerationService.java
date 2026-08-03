package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.client.ImprovementGuideClient;
import com.aivle13.fin_audit_ai.global.ai.dto.ImprovementGuideResponse;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 개선 권고 가이드 생성.
 *
 * <p>조치 필요 항목은 {@link ImprovementGuideRequestAssembler} 가 한 트랜잭션에서
 * 조립하고, 우선순위는 AI 서버가 산정한다. 이 서비스는 AI 호출과 산출물 저장만 담당한다.
 */
@Service
@RequiredArgsConstructor
public class ImprovementGuideGenerationService {

    private final ImprovementGuideRequestAssembler requestAssembler;
    private final ImprovementGuideClient reportClient;
    private final ReportPersistenceService reportPersistenceService;
    private final ReportNarrativePersistenceService narrativePersistenceService;

    public Long generateAndSave(
            Long userId,
            Long auditId
    ) {
        // 조립은 짧은 트랜잭션으로 끝내고, 수십 초 걸리는 AI 호출은 트랜잭션 밖에서 한다.
        ImprovementGuideResponse response =
                reportClient.generate(
                        requestAssembler.assemble(userId, auditId)
                );

        validateResponse(auditId, response);

        // HTML·PDF·Word를 한 트랜잭션으로 저장한다. 하나라도 실패하면 업로드된
        // S3 파일까지 함께 정리된다.
        // 챗봇이 리포트 내용을 근거로 답할 수 있도록 섹션별 서술을 저장한다.
        // 저장 실패가 리포트 생성 자체를 막지 않도록 예외는 삼킨다.
        narrativePersistenceService.saveQuietly(
                auditId,
                ReportType.IMPROVEMENT_GUIDE,
                response.narratives()
        );

        Map<ReportFormat, Long> reportIds =
                reportPersistenceService.saveAll(
                        auditId,
                        ReportType.IMPROVEMENT_GUIDE,
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

        return htmlReportId;
    }

    private void validateResponse(
            Long auditId,
            ImprovementGuideResponse response
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
