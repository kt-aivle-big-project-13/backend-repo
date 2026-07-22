package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.ExplainabilityResultRequest;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.global.ai.client.ShapAnalysisClient;
import com.aivle13.fin_audit_ai.global.ai.dto.ShapAnalysisRequest;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShapAnalysisService {

    private static final String DEFAULT_TARGET_COLUMN = "TARGET";

    private final AuditRepository auditRepository;
    private final ShapAnalysisClient shapAnalysisClient;
    private final ExplainabilityService explainabilityService;

    public void analyzeAndSave(Long auditId) {
        AuditEntity audit = auditRepository
                .findByIdWithModelAndDataset(auditId)
                .orElseThrow(AuditNotFoundException::new);

        ShapAnalysisRequest request = createRequest(audit);

        ExplainabilityResultRequest response =
                shapAnalysisClient.analyze(request);

        explainabilityService.saveExplainabilityResult(
                auditId,
                response
        );
    }

    private ShapAnalysisRequest createRequest(
            AuditEntity audit
    ) {
        String modelS3Key =
                audit.getModel().getArtifactPath();

        String datasetS3Key =
                audit.getDataset().getDatasetFileKey();

        validateS3Key(modelS3Key);
        validateS3Key(datasetS3Key);

        return new ShapAnalysisRequest(
                audit.getId(),
                modelS3Key,
                datasetS3Key,
                DEFAULT_TARGET_COLUMN,
                parseSensitiveFeatures(
                        audit.getSensitiveFeatures()
                )
        );
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