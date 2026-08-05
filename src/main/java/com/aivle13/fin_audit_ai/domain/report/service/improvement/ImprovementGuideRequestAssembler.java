package com.aivle13.fin_audit_ai.domain.report.service.improvement;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.compliance.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiStatus;
import com.aivle13.fin_audit_ai.domain.law.dto.AuditRegulationComplianceView;
import com.aivle13.fin_audit_ai.domain.law.service.mapping.AuditRegulationMappingQueryService;
import com.aivle13.fin_audit_ai.global.ai.dto.report.request.ImprovementGuideRequest;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 개선 권고 가이드의 조치 필요 항목을 한 트랜잭션에서 조립한다.
 *
 * <p>판정하지 않는다. 임계값 판정이 끝나 저장된 결과 중 조치가 필요한 것만 골라 담는다.
 * 우선순위는 AI 서버가 코드로 산정하므로 값과 상태만 전달한다.
 *
 * <p>생성 서비스와 분리한 이유는 규제준수 판정서와 같다. 같은 클래스 안에서 호출하면
 * 프록시를 거치지 않아 {@code @Transactional} 이 적용되지 않고, AI 호출이 수십 초 걸려
 * 그 시간까지 트랜잭션을 열어둘 수 없다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImprovementGuideRequestAssembler {

    private final AuditRepository auditRepository;
    private final SelfCheckAnswerRepository selfCheckAnswerRepository;
    private final FairnessResultRepository fairnessResultRepository;
    private final XaiResultRepository xaiResultRepository;
    private final AuditRegulationMappingQueryService regulationMappingQueryService;

    public ImprovementGuideRequest assemble(
            Long userId,
            Long auditId
    ) {
        AuditEntity audit = auditRepository
                .findByIdAndUser_IdWithModelAndDataset(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);

        return new ImprovementGuideRequest(
                audit.getId(),
                audit.getAuditName(),
                audit.getModel().getModelName(),
                createComplianceGaps(auditId),
                createSelfCheckGaps(auditId),
                createSelfCheckRecommendations(auditId),
                createFairnessFindings(auditId),
                createExplainabilityFindings(auditId)
        );
    }

    private List<ImprovementGuideRequest.ComplianceGap> createComplianceGaps(
            Long auditId
    ) {
        return regulationMappingQueryService.getMappings(auditId)
                .stream()
                .filter(view -> view.compliance() == ComplianceStatus.NON_COMPLIANT)
                .map(ImprovementGuideRequestAssembler::toComplianceGap)
                .toList();
    }

    private static ImprovementGuideRequest.ComplianceGap toComplianceGap(
            AuditRegulationComplianceView view
    ) {
        // AuditRegulationComplianceView 의 articleTitle 은 법령명이다.
        return new ImprovementGuideRequest.ComplianceGap(
                view.articleTitle(),
                view.articleNumber(),
                view.summary(),
                view.evidence()
        );
    }

    // 노력의무가 아닌 항목만 담는다 — 노력의무는 '아니오'여도 위반이 아니라서 여기 섞이면
    // 안 된다(createSelfCheckRecommendations로 분리).
    private List<ImprovementGuideRequest.SelfCheckGap> createSelfCheckGaps(
            Long auditId
    ) {
        return selfCheckAnswerRepository.findAllByAudit_Id(auditId)
                .stream()
                .filter(SelfCheckAnswerEntity::isNo)
                .filter(answer -> !answer.getItemCode().isEffortObligation())
                .map(ImprovementGuideRequestAssembler::toSelfCheckGap)
                .toList();
    }

    // 노력의무 문항에서 '아니오'로 답한 것들. 위반이 아니라 권장 사항이라 별도 섹션으로 뺀다.
    private List<ImprovementGuideRequest.SelfCheckGap> createSelfCheckRecommendations(
            Long auditId
    ) {
        return selfCheckAnswerRepository.findAllByAudit_Id(auditId)
                .stream()
                .filter(SelfCheckAnswerEntity::isNo)
                .filter(answer -> answer.getItemCode().isEffortObligation())
                .map(ImprovementGuideRequestAssembler::toSelfCheckGap)
                .toList();
    }

    private static ImprovementGuideRequest.SelfCheckGap toSelfCheckGap(
            SelfCheckAnswerEntity answer
    ) {
        return new ImprovementGuideRequest.SelfCheckGap(
                answer.getItemCode().code(),
                answer.getItemCode().label()
        );
    }

    private List<ImprovementGuideRequest.MetricFinding> createFairnessFindings(
            Long auditId
    ) {
        return fairnessResultRepository.findAllByAudit_Id(auditId)
                .stream()
                .filter(result -> result.getStatus() != FairnessStatus.PASS)
                .map(result -> new ImprovementGuideRequest.MetricFinding(
                        result.getAttribute(),
                        result.getMetricCode().name(),
                        result.getValue(),
                        result.getThreshold(),
                        result.getStatus().name()
                ))
                .toList();
    }

    private List<ImprovementGuideRequest.MetricFinding> createExplainabilityFindings(
            Long auditId
    ) {
        // 설명가능성은 모델 전체 단위라 보호속성이 없다.
        return xaiResultRepository.findAllByAudit_Id(auditId)
                .stream()
                .filter(result -> result.getStatus() != XaiStatus.PASS)
                .map(result -> new ImprovementGuideRequest.MetricFinding(
                        null,
                        result.getMetricCode().name(),
                        result.getValue(),
                        result.getThreshold(),
                        result.getStatus().name()
                ))
                .toList();
    }
}
