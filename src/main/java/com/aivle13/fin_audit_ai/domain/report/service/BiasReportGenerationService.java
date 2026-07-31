package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.client.BiasReportClient;
import com.aivle13.fin_audit_ai.global.ai.dto.BiasReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.BiasReportResponse;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BiasReportGenerationService {

    private final AuditRepository auditRepository;
    private final BiasReportClient reportClient;
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

        BiasReportResponse response =
                reportClient.generate(createRequest(audit));

        validateResponse(auditId, response);

        return reportPersistenceService.save(
                auditId,
                ReportType.BIAS_REPORT,
                ReportFormat.HTML,
                response.reportS3Key()
        );
    }

    private BiasReportRequest createRequest(
            AuditEntity audit
    ) {
        String modelS3Key =
                audit.getModel().getArtifactPath();

        String datasetS3Key =
                audit.getDataset().getDatasetFileKey();

        validateS3Key(modelS3Key);
        validateS3Key(datasetS3Key);

        // 검증 데이터셋은 선택 항목이라 없으면 null로 보낸다. 다만 데이터셋이 있는데
        // 파일 키가 비어 있으면(dataSource=DUMMY) 그대로 null이 되어 AI 서버가 감사셋
        // 기준으로 조용히 폴백하므로, 그 경우는 감사 자체가 잘못된 것으로 보고 막는다.
        String validationDatasetS3Key = null;

        if (audit.getValidationDataset() != null) {
            validationDatasetS3Key =
                    audit.getValidationDataset()
                            .getDatasetFileKey();

            validateS3Key(validationDatasetS3Key);
        }

        // 임계값 설정을 넘기지 않으면 AI 서버가 기본 목표 승인율로 다시 계산해
        // 같은 감사의 공정성 결과와 다른 기준으로 리포트가 생성된다.
        return new BiasReportRequest(
                audit.getId(),
                modelS3Key,
                datasetS3Key,
                validationDatasetS3Key,
                audit.getAuditName(),
                audit.getTargetApprovalRate(),
                audit.getManualThreshold(),
                parseSensitiveFeatures(
                        audit.getSensitiveFeatures()
                )
        );
    }

    private void validateResponse(
            Long auditId,
            BiasReportResponse response
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
