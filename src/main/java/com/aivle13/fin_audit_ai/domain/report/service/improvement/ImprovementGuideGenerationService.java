package com.aivle13.fin_audit_ai.domain.report.service.improvement;

import com.aivle13.fin_audit_ai.domain.report.service.common.ReportGenerationGuard;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportNarrativeRecorder;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportPersistenceService;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.client.report.ImprovementGuideClient;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.ImprovementGuideResponse;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditFailedException;
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
    private final ReportNarrativeRecorder narrativeRecorder;
    private final ReportGenerationGuard generationGuard;

    // 이미 만들어져 있거나 만드는 중이면 AI 호출 없이 그 결과를 쓴다.
    public Long generateAndSave(
            Long userId,
            Long auditId
    ) {
        return generationGuard.generateOnce(
                userId,
                auditId,
                ReportType.IMPROVEMENT_GUIDE,
                ReportFormat.HTML,
                () -> doGenerateAndSave(userId, auditId)
        );
    }

    private Long doGenerateAndSave(
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

        // 리포트 저장이 끝난 뒤에 서술을 남긴다. 먼저 저장하면 리포트 저장이 실패했을 때
        // 존재하지 않는 리포트의 서술을 챗봇이 근거로 인용하게 된다.
        narrativeRecorder.record(
                auditId,
                ReportType.IMPROVEMENT_GUIDE,
                response.narratives()
        );

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
