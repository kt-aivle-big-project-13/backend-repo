package com.aivle13.fin_audit_ai.domain.report.service.explainability;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportNarrativeRecorder;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportPersistenceService;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.client.report.ExplainabilityReportClient;
import com.aivle13.fin_audit_ai.global.ai.dto.report.request.ExplainabilityReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.ExplainabilityReportResponse;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExplainabilityReportGenerationService {

    private static final String DEFAULT_TARGET_COLUMN = "TARGET";
    private static final int DEFAULT_REPORT_TOP_N = 20;

    private final AuditRepository auditRepository;
    private final ExplainabilityReportClient reportClient;
    private final ReportPersistenceService reportPersistenceService;
    private final ReportNarrativeRecorder narrativeRecorder;

    public Long generateAndSave(
            Long userId,
            Long auditId
    ) {
        AuditEntity audit = auditRepository
                .findByIdAndUser_IdWithModelAndDataset(
                        auditId,
                        userId
                )
                .orElseThrow(AuditNotFoundException::new);

        ExplainabilityReportResponse response =
                reportClient.generate(createRequest(audit));

        validateResponse(auditId, response);

        Map<ReportFormat, Long> reportIds =
                reportPersistenceService.saveAll(
                        auditId,
                        ReportType.XAI_REPORT,
                        Map.of(
                                ReportFormat.HTML,
                                response.reportS3Key(),
                                ReportFormat.PDF,
                                response.pdfReportS3Key(),
                                ReportFormat.WORD,
                                response.wordReportS3Key()
                        )
                );

        Long htmlReportId =
                reportIds.get(ReportFormat.HTML);

        if (htmlReportId == null) {
            throw new AuditFailedException();
        }

        // 리포트 저장이 끝난 뒤에 서술을 남긴다. 먼저 저장하면 리포트 저장이 실패했을 때
        // 존재하지 않는 리포트의 서술을 챗봇이 근거로 인용하게 된다.
        narrativeRecorder.record(
                auditId,
                ReportType.XAI_REPORT,
                response.narratives()
        );

        return htmlReportId;
    }

    private ExplainabilityReportRequest createRequest(
            AuditEntity audit
    ) {
        String modelS3Key =
                audit.getModel().getArtifactPath();

        String datasetS3Key =
                audit.getDataset().getDatasetFileKey();

        validateS3Key(modelS3Key);
        validateS3Key(datasetS3Key);

        return new ExplainabilityReportRequest(
                audit.getId(),
                modelS3Key,
                datasetS3Key,
                DEFAULT_TARGET_COLUMN,
                parseSensitiveFeatures(
                        audit.getSensitiveFeatures()
                ),
                DEFAULT_REPORT_TOP_N
        );
    }

    private void validateResponse(
            Long auditId,
            ExplainabilityReportResponse response
    ) {
        if (response == null
                || !auditId.equals(response.auditId())
                || response.reportS3Key() == null
                || response.reportS3Key().isBlank()
                || response.pdfReportS3Key() == null
                || response.pdfReportS3Key().isBlank()
                || response.wordReportS3Key() == null
                || response.wordReportS3Key().isBlank()
                || response.format() == null
                || !"html".equalsIgnoreCase(
                        response.format().trim()
                )) {
            throw new AuditFailedException();
        }
    }

    private List<String> parseSensitiveFeatures(
            String sensitiveFeatures
    ) {
        if (sensitiveFeatures == null) {
            throw new AuditFailedException();
        }

        List<String> features = Arrays.stream(
                        sensitiveFeatures.split(",")
                )
                .map(String::trim)
                .filter(feature -> !feature.isBlank())
                .distinct()
                .toList();

        if (features.isEmpty()) {
            throw new AuditFailedException();
        }

        return features;
    }

    private void validateS3Key(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            throw new AuditFailedException();
        }
    }
}
