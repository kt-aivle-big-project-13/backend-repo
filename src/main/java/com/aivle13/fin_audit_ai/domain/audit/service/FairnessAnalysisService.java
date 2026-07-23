package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.FairnessRunRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.FairnessRunResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import com.aivle13.fin_audit_ai.global.ai.client.FairnessAnalysisClient;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FairnessAnalysisService {

    private final AuditRepository auditRepository;
    private final DatasetRepository datasetRepository;
    private final FairnessAnalysisClient fairnessAnalysisClient;
    private final FairnessResultService fairnessResultService;

    public void analyzeAndSave(Long auditId) {
        AuditEntity audit = auditRepository
                .findByIdWithModelAndDataset(auditId)
                .orElseThrow(AuditNotFoundException::new);

        FairnessRunRequest request = createRequest(audit);

        FairnessRunResponse response =
                fairnessAnalysisClient.analyze(request);

        fairnessResultService.saveFairnessResult(auditId, response);
    }

    private FairnessRunRequest createRequest(AuditEntity audit) {
        String modelFileKey = audit.getModel().getArtifactPath();
        String auditDatasetFileKey = audit.getDataset().getDatasetFileKey();

        validateS3Key(modelFileKey);
        validateS3Key(auditDatasetFileKey);

        validateSensitiveFeatures(audit.getSensitiveFeatures());

        String validationDatasetFileKey = findValidationDatasetFileKey(
                audit.getModel().getId()
        );

        return new FairnessRunRequest(
                audit.getId(),
                modelFileKey,
                auditDatasetFileKey,
                validationDatasetFileKey,
                audit.getAuditName(),
                audit.getTargetApprovalRate(),
                audit.getManualThreshold(),
                audit.getSensitiveFeatures()
        );
    }

    private String findValidationDatasetFileKey(Long modelId) {
        return datasetRepository
                .findFirstByModel_IdAndPurposeOrderByCreatedAtDesc(
                        modelId,
                        DatasetPurpose.VALIDATION
                )
                .map(DatasetEntity::getDatasetFileKey)
                .orElse(null);
    }

    private void validateSensitiveFeatures(String sensitiveFeatures) {
        if (sensitiveFeatures == null) {
            throw new AuditFailedException();
        }

        List<String> features = Arrays.stream(sensitiveFeatures.split(","))
                .map(String::trim)
                .filter(feature -> !feature.isBlank())
                .distinct()
                .toList();

        if (features.isEmpty()) {
            throw new AuditFailedException();
        }
    }

    private void validateS3Key(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            throw new AuditFailedException();
        }
    }
}
