package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.law.dto.AuditRegulationComplianceView;
import com.aivle13.fin_audit_ai.domain.law.service.AuditRegulationMappingQueryService;
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
    private final AuditRegulationMappingQueryService auditRegulationMappingQueryService;

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

        List<AuditRegulationComplianceView> regulationCompliances =
                auditRegulationMappingQueryService.getMappings(auditId);

        // 개선 권고 조회 또는 생성 기능이 구현되면 실제 데이터를 연결한다.
        // 현재는 확정되지 않은 권고 내용을 임의로 생성하지 않기 위해 빈 목록을 사용한다.
        List<String> improvementGuides = List.of();

        return new ReportGenerationContext(
                auditId,
                xaiResults,
                fairnessResults,
                regulationCompliances,
                improvementGuides
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
}