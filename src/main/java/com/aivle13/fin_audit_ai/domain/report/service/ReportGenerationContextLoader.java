package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.law.dto.AuditLawComplianceView;
import com.aivle13.fin_audit_ai.domain.law.service.AuditLawMappingQueryService;
import com.aivle13.fin_audit_ai.domain.report.dto.AuditMetricView;
import com.aivle13.fin_audit_ai.domain.report.dto.ReportGenerationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportGenerationContextLoader {

    private final XaiResultRepository xaiResultRepository;
    private final FairnessResultRepository fairnessResultRepository;
    private final SelfCheckAnswerRepository selfCheckAnswerRepository;
    private final AuditLawMappingQueryService auditLawMappingQueryService;

    // 감사 결과와 법령 매핑을 보고서 생성 컨텍스트로 변환
    public ReportGenerationContext load(Long auditId) {
        List<AuditMetricView> xaiResults = xaiResultRepository
                .findAllByAudit_Id(auditId)
                .stream()
                .map(this::toXaiMetricView)
                .toList();

        List<AuditMetricView> fairnessResults = fairnessResultRepository
                .findAllByAudit_Id(auditId)
                .stream()
                .map(this::toFairnessMetricView)
                .toList();

        List<AuditMetricView> selfCheckResults = selfCheckAnswerRepository
                .findAllByAudit_Id(auditId)
                .stream()
                .map(this::toSelfCheckMetricView)
                .toList();

        List<AuditLawComplianceView> lawCompliances =
                auditLawMappingQueryService.getMappings(auditId);

        return new ReportGenerationContext(
                auditId,
                xaiResults,
                fairnessResults,
                selfCheckResults,
                lawCompliances,
                List.of()
        );
    }

    // XAI 결과를 보고서 공통 지표 형식으로 변환
    private AuditMetricView toXaiMetricView(XaiResultEntity result) {
        return new AuditMetricView(
                result.getMetricCode().name(),
                result.getValue().toPlainString(),
                result.getStatus().name(),
                "기준값: " + result.getThreshold().toPlainString()
        );
    }

    // 편향 진단 결과를 보고서 공통 지표 형식으로 변환
    private AuditMetricView toFairnessMetricView(
            FairnessResultEntity result
    ) {
        return new AuditMetricView(
                result.getAttribute() + " / " + result.getMetricCode().name(),
                result.getValue().toPlainString(),
                result.getStatus().name(),
                "기준값: " + result.getThreshold().toPlainString()
        );
    }

    // 자가진단 결과를 보고서 공통 지표 형식으로 변환
    private AuditMetricView toSelfCheckMetricView(
            SelfCheckAnswerEntity answer
    ) {
        return new AuditMetricView(
                answer.getItemCode().name(),
                Boolean.toString(answer.isAnswer()),
                answer.isAnswer() ? "PASS" : "FAIL",
                answer.isAnswer()
                        ? "자가진단 항목 충족"
                        : "자가진단 항목 미충족"
        );
    }
}