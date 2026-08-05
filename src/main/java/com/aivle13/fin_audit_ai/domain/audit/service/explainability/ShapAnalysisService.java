package com.aivle13.fin_audit_ai.domain.audit.service.explainability;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.explainability.ExplainabilityResultRequest;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.global.ai.client.analysis.ShapAnalysisClient;
import com.aivle13.fin_audit_ai.global.ai.dto.analysis.request.ShapAnalysisRequest;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShapAnalysisService {

    private static final String DEFAULT_TARGET_COLUMN = "TARGET";
    // 대시보드 "예측 영향 변수 TOP5" 카드에 쓸 전역 피처 중요도 개수
    private static final int FEATURE_IMPORTANCE_TOP_N = 5;

    private final AuditRepository auditRepository;
    private final ShapAnalysisClient shapAnalysisClient;
    private final ExplainabilityService explainabilityService;

    public void analyzeAndSave(Long auditId, int generation) {
        AuditEntity audit = auditRepository
                .findByIdWithModelAndDataset(auditId)
                .orElseThrow(AuditNotFoundException::new);

        ShapAnalysisRequest request = createRequest(audit);

        ExplainabilityResultRequest response =
                shapAnalysisClient.analyze(request);

        explainabilityService.saveExplainabilityResult(
                auditId,
                generation,
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
                ),
                true,
                FEATURE_IMPORTANCE_TOP_N
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