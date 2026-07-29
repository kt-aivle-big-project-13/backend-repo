package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.law.dto.AuditRegulationComplianceView;
import com.aivle13.fin_audit_ai.domain.law.service.AuditRegulationMappingQueryService;
import com.aivle13.fin_audit_ai.domain.report.dto.ReportGenerationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReportGenerationContextLoaderTest {

    private static final Long AUDIT_ID = 21L;

    @Mock
    private XaiResultRepository xaiResultRepository;

    @Mock
    private FairnessResultRepository fairnessResultRepository;

    @Mock
    private SelfCheckAnswerRepository selfCheckAnswerRepository;

    @Mock
    private AuditRegulationMappingQueryService auditRegulationMappingQueryService;

    @InjectMocks
    private ReportGenerationContextLoader contextLoader;

    @Test
    void loadsRegulationCompliancesFromQueryService() {
        given(xaiResultRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(List.of());
        given(fairnessResultRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(List.of());
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(List.of());

        AuditRegulationComplianceView view = new AuditRegulationComplianceView(
                1L, "제12조", "인공지능 기본법", ComplianceStatus.NON_COMPLIANT, "근거"
        );
        given(auditRegulationMappingQueryService.getMappings(AUDIT_ID)).willReturn(List.of(view));

        ReportGenerationContext context = contextLoader.load(AUDIT_ID);

        assertThat(context.auditId()).isEqualTo(AUDIT_ID);
        assertThat(context.regulationCompliances()).containsExactly(view);
    }
}
