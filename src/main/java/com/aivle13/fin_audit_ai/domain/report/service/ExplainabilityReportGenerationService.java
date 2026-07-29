package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.client.ExplainabilityReportClient;
import com.aivle13.fin_audit_ai.global.ai.dto.ExplainabilityReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.ExplainabilityReportResponse;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExplainabilityReportGenerationService {

    private static final String DEFAULT_TARGET_COLUMN = "TARGET";
    private static final int DEFAULT_REPORT_TOP_N = 20;

    private final AuditRepository auditRepository;
    private final ExplainabilityReportClient reportClient;
    private final ReportPersistenceService reportPersistenceService;

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

        return reportPersistenceService.save(
                auditId,
                ReportType.XAI_REPORT,
                ReportFormat.HTML,
                response.reportS3Key()
        );
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
