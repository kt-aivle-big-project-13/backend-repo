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
import com.aivle13.fin_audit_ai.global.ai.dto.ComplianceReportRequest;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 규제준수 판정서의 판정 근거를 한 트랜잭션에서 조립한다.
 *
 * <p>생성 서비스와 분리한 이유는 두 가지다. 하나는 같은 클래스 안에서 호출하면 프록시를
 * 거치지 않아 {@code @Transactional} 이 적용되지 않기 때문이고, 다른 하나는 AI 서버 호출이
 * 수십 초 걸려 그 시간까지 트랜잭션을 열어두면 안 되기 때문이다. 조립만 짧게 트랜잭션으로
 * 묶고, AI 호출은 트랜잭션 밖에서 한다.
 *
 * <p>자가점검 응답과 법령 매핑은 재제출 시 delete+insert 로 갱신되므로, 한 트랜잭션에서
 * 함께 읽어야 서로 다른 시점의 값이 섞이지 않는다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplianceReportRequestAssembler {

    private final AuditRepository auditRepository;
    private final SelfCheckAnswerRepository selfCheckAnswerRepository;
    private final FairnessResultRepository fairnessResultRepository;
    private final AuditRegulationMappingQueryService regulationMappingQueryService;

    public ComplianceReportRequest assemble(
            Long userId,
            Long auditId
    ) {
        AuditEntity audit = auditRepository
                .findByIdAndUser_IdWithModelAndDataset(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);

        // AI 서버 요청 스키마의 answer는 boolean 고정이라 "해당없음"을 담을 수 없다 —
        // 해당없음은 애초에 판정 대상이 아니므로(적용 여부 자체가 아니라는 뜻) 판정 근거
        // 목록에서 제외한다.
        List<ComplianceReportRequest.SelfCheckAnswer> answers =
                selfCheckAnswerRepository.findAllByAudit_Id(auditId)
                        .stream()
                        .filter(answer -> !answer.isNa())
                        .map(ComplianceReportRequestAssembler::toAnswer)
                        .toList();

        // 자가점검을 아직 작성하지 않았으면 판정 근거가 없어 판정서를 만들 수 없다.
        if (answers.isEmpty()) {
            throw new AuditFailedException();
        }

        List<ComplianceReportRequest.RegulationMapping> mappings =
                regulationMappingQueryService.getMappings(auditId)
                        .stream()
                        .map(ComplianceReportRequestAssembler::toMapping)
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

    private static ComplianceReportRequest.SelfCheckAnswer toAnswer(
            SelfCheckAnswerEntity answer
    ) {
        return new ComplianceReportRequest.SelfCheckAnswer(
                answer.getItemCode().code(),
                answer.getItemCode().label(),
                answer.isYes()
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
     *
     * <p>고객 수와 전체 승인율은 AI 분석 응답에는 있으나 저장되지 않아 비워 둔다. 집단
     * 통계를 합산하면 근사치는 얻을 수 있지만, 표본 부족으로 제외된 집단은 저장되지 않아
     * 실제보다 적게 집계된다. 규제 문서에 부정확한 수치를 넣지 않기 위해 비워 두고,
     * 감사에 값을 저장하는 것은 별도로 다룬다.
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
}
