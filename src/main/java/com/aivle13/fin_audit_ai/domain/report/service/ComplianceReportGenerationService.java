package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.law.dto.AuditRegulationComplianceView;
import com.aivle13.fin_audit_ai.domain.law.service.AuditRegulationMappingQueryService;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.client.ComplianceReportClient;
import com.aivle13.fin_audit_ai.global.ai.dto.ComplianceReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.ComplianceReportResponse;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 규제준수 판정서 생성.
 *
 * <p>판정은 여기서 내리지 않는다. 조항별 준수/미준수는 {@code AuditRegulationMappingService}
 * 가 자가점검 응답으로 이미 정해 저장해 뒀고, 이 서비스는 그 결과와 참고용 감사 요약을
 * 조립해 AI 서버에 넘긴다.
 */
@Service
@RequiredArgsConstructor
public class ComplianceReportGenerationService {

    private final AuditRepository auditRepository;
    private final SelfCheckAnswerRepository selfCheckAnswerRepository;
    private final FairnessResultRepository fairnessResultRepository;
    private final AuditRegulationMappingQueryService regulationMappingQueryService;
    private final ComplianceReportClient reportClient;
    private final ReportPersistenceService reportPersistenceService;

    @Transactional(readOnly = true)
    public ComplianceReportRequest createRequest(
            Long userId,
            Long auditId
    ) {
        AuditEntity audit = auditRepository
                .findByIdAndUser_IdWithModelAndDataset(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);

        List<ComplianceReportRequest.SelfCheckAnswer> answers =
                selfCheckAnswerRepository.findAllByAudit_Id(auditId)
                        .stream()
                        .map(ComplianceReportGenerationService::toAnswer)
                        .toList();

        // 자가점검을 아직 작성하지 않았으면 판정 근거가 없어 판정서를 만들 수 없다.
        if (answers.isEmpty()) {
            throw new AuditFailedException();
        }

        List<ComplianceReportRequest.RegulationMapping> mappings =
                regulationMappingQueryService.getMappings(auditId)
                        .stream()
                        .map(ComplianceReportGenerationService::toMapping)
                        .toList();

        if (mappings.isEmpty()) {
            throw new AuditFailedException();
        }

        return new ComplianceReportRequest(
                audit.getId(),
                audit.getAuditName(),
                audit.getModel().getModelName(),
                answers,
                mappings,
                createAuditReference(audit, auditId)
        );
    }

    public Long generateAndSave(
            Long userId,
            Long auditId
    ) {
        ComplianceReportResponse response =
                reportClient.generate(createRequest(userId, auditId));

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

        return htmlReportId;
    }

    private static ComplianceReportRequest.SelfCheckAnswer toAnswer(
            SelfCheckAnswerEntity answer
    ) {
        return new ComplianceReportRequest.SelfCheckAnswer(
                answer.getItemCode().name(),
                answer.getItemCode().label(),
                answer.isAnswer()
        );
    }

    private static ComplianceReportRequest.RegulationMapping toMapping(
            AuditRegulationComplianceView view
    ) {
        // AuditRegulationComplianceView 의 articleTitle 은 법령명이다.
        // 시행일·개정일은 이 조회 결과에 없어 비워 둔다(AI 스키마에서 선택 항목).
        return new ComplianceReportRequest.RegulationMapping(
                view.articleTitle(),
                view.articleNumber(),
                view.content(),
                view.summary(),
                view.compliance().name(),
                view.evidence(),
                null,
                null
        );
    }

    /**
     * 참고용 감사 요약을 만든다. 판정 근거가 아니므로 값이 없으면 그대로 비워 둔다.
     */
    private ComplianceReportRequest.AuditReference createAuditReference(
            AuditEntity audit,
            Long auditId
    ) {
        return new ComplianceReportRequest.AuditReference(
                null,
                null,
                audit.getManualThreshold(),
                audit.getThresholdMethod() != null
                        ? audit.getThresholdMethod().name()
                        : null,
                audit.getModelAuc(),
                audit.getModelAccuracy(),
                createFairnessReferences(auditId)
        );
    }

    private List<ComplianceReportRequest.FairnessReference> createFairnessReferences(
            Long auditId
    ) {
        Map<String, Map<FairnessMetricCode, BigDecimal>> byAttribute =
                new LinkedHashMap<>();

        for (FairnessResultEntity result
                : fairnessResultRepository.findAllByAudit_Id(auditId)) {
            byAttribute
                    .computeIfAbsent(
                            result.getAttribute(),
                            key -> new LinkedHashMap<>()
                    )
                    .put(result.getMetricCode(), result.getValue());
        }

        return byAttribute.entrySet()
                .stream()
                .map(entry -> new ComplianceReportRequest.FairnessReference(
                        entry.getKey(),
                        entry.getValue().get(FairnessMetricCode.DEMOGRAPHIC_PARITY),
                        entry.getValue().get(FairnessMetricCode.EQUAL_OPPORTUNITY),
                        entry.getValue().get(FairnessMetricCode.EQUALIZED_ODDS),
                        entry.getValue().get(FairnessMetricCode.PROPORTIONAL_PARITY)
                ))
                .toList();
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
