package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.compliance.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.selfcheck.SelfCheckAnswerValue;
import com.aivle13.fin_audit_ai.domain.audit.type.selfcheck.SelfCheckItemCode;
import com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiStatus;
import com.aivle13.fin_audit_ai.domain.law.dto.AuditRegulationComplianceView;
import com.aivle13.fin_audit_ai.domain.law.service.mapping.AuditRegulationMappingQueryService;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.report.service.improvement.ImprovementGuideRequestAssembler;
import com.aivle13.fin_audit_ai.global.ai.dto.report.request.ImprovementGuideRequest;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditNotFoundException;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ImprovementGuideRequestAssemblerTest {

    private static final Long USER_ID = 2L;
    private static final Long AUDIT_ID = 77L;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private SelfCheckAnswerRepository selfCheckAnswerRepository;

    @Mock
    private FairnessResultRepository fairnessResultRepository;

    @Mock
    private XaiResultRepository xaiResultRepository;

    @Mock
    private AuditRegulationMappingQueryService regulationMappingQueryService;

    @Mock
    private AuditEntity audit;

    @Mock
    private AiModelEntity model;

    @InjectMocks
    private ImprovementGuideRequestAssembler assembler;

    @Test
    void includesOnlyNonCompliantArticles() {
        givenAudit();
        givenEmptySelfCheck();
        givenEmptyFairness();
        givenEmptyXai();

        given(regulationMappingQueryService.getMappings(AUDIT_ID))
                .willReturn(List.of(
                        mapping("신용정보법", "제36조의2", ComplianceStatus.NON_COMPLIANT),
                        mapping("AI 기본법", "제27조", ComplianceStatus.COMPLIANT),
                        mapping("보류법", "제1조", ComplianceStatus.PENDING)
                ));

        ImprovementGuideRequest request =
                assembler.assemble(USER_ID, AUDIT_ID);

        // 준수·보류 조항은 개선 대상이 아니다.
        assertThat(request.complianceGaps())
                .extracting(
                        ImprovementGuideRequest.ComplianceGap::lawName,
                        ImprovementGuideRequest.ComplianceGap::articleNo
                )
                .containsExactly(Tuple.tuple("신용정보법", "제36조의2"));
    }

    @Test
    void includesOnlyUnmetSelfCheckItems() {
        givenAudit();
        givenEmptyMappings();
        givenEmptyFairness();
        givenEmptyXai();

        SelfCheckAnswerEntity met = answer(SelfCheckItemCode.TR_01, SelfCheckAnswerValue.YES);
        SelfCheckAnswerEntity unmet = answer(SelfCheckItemCode.UP_01, SelfCheckAnswerValue.NO);

        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(met, unmet));

        ImprovementGuideRequest request =
                assembler.assemble(USER_ID, AUDIT_ID);

        assertThat(request.selfCheckGaps())
                .extracting(ImprovementGuideRequest.SelfCheckGap::itemCode)
                .containsExactly("UP-01");
        assertThat(request.selfCheckRecommendations()).isEmpty();
    }

    // 노력의무 문항(IA-01 등)에서 '아니오'로 답하면 개선 권고가 아니라 참고 권고로 분리된다.
    @Test
    void includesEffortObligationNoAnswersAsRecommendationsOnly() {
        givenAudit();
        givenEmptyMappings();
        givenEmptyFairness();
        givenEmptyXai();

        SelfCheckAnswerEntity effortNo = answer(SelfCheckItemCode.IA_01, SelfCheckAnswerValue.NO);

        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(effortNo));

        ImprovementGuideRequest request =
                assembler.assemble(USER_ID, AUDIT_ID);

        assertThat(request.selfCheckGaps()).isEmpty();
        assertThat(request.selfCheckRecommendations())
                .extracting(ImprovementGuideRequest.SelfCheckGap::itemCode)
                .containsExactly("IA-01");
    }

    @Test
    void includesOnlyFairnessResultsAboveThreshold() {
        givenAudit();
        givenEmptyMappings();
        givenEmptySelfCheck();
        givenEmptyXai();

        FairnessResultEntity passed = fairnessResult(
                FairnessMetricCode.DEMOGRAPHIC_PARITY,
                FairnessStatus.PASS
        );
        FairnessResultEntity review = fairnessResult(
                FairnessMetricCode.EQUAL_OPPORTUNITY,
                FairnessStatus.REVIEW
        );
        FairnessResultEntity failed = fairnessResult(
                FairnessMetricCode.EQUALIZED_ODDS,
                FairnessStatus.FAIL
        );

        given(fairnessResultRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(passed, review, failed));

        ImprovementGuideRequest request =
                assembler.assemble(USER_ID, AUDIT_ID);

        assertThat(request.fairnessFindings())
                .extracting(
                        ImprovementGuideRequest.MetricFinding::metricCode,
                        ImprovementGuideRequest.MetricFinding::status
                )
                .containsExactly(
                        Tuple.tuple("EQUAL_OPPORTUNITY", "REVIEW"),
                        Tuple.tuple("EQUALIZED_ODDS", "FAIL")
                );

        // 공정성은 보호속성 단위라 attribute 가 채워진다.
        assertThat(request.fairnessFindings().get(0).attribute())
                .isEqualTo("CODE_GENDER");
    }

    @Test
    void includesOnlyExplainabilityResultsAboveThreshold() {
        givenAudit();
        givenEmptyMappings();
        givenEmptySelfCheck();
        givenEmptyFairness();

        XaiResultEntity passed = xaiResult(XaiStatus.PASS);
        XaiResultEntity warning = xaiResult(XaiStatus.WARNING);

        given(xaiResultRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(passed, warning));

        ImprovementGuideRequest request =
                assembler.assemble(USER_ID, AUDIT_ID);

        assertThat(request.explainabilityFindings()).hasSize(1);
        assertThat(request.explainabilityFindings().get(0).status())
                .isEqualTo("WARNING");

        // 설명가능성은 모델 전체 단위라 보호속성이 없다.
        assertThat(request.explainabilityFindings().get(0).attribute()).isNull();
    }

    @Test
    void assemblesEmptyRequestWhenNothingNeedsAction() {
        givenAudit();
        givenEmptyMappings();
        givenEmptySelfCheck();
        givenEmptyFairness();
        givenEmptyXai();

        ImprovementGuideRequest request =
                assembler.assemble(USER_ID, AUDIT_ID);

        // 조치할 항목이 없어도 가이드는 생성한다(AI 가 "해당 없음"으로 표기).
        assertThat(request.complianceGaps()).isEmpty();
        assertThat(request.selfCheckGaps()).isEmpty();
        assertThat(request.selfCheckRecommendations()).isEmpty();
        assertThat(request.fairnessFindings()).isEmpty();
        assertThat(request.explainabilityFindings()).isEmpty();
        assertThat(request.auditId()).isEqualTo(AUDIT_ID);
    }

    @Test
    void throwsWhenAuditDoesNotExist() {
        given(auditRepository.findByIdAndUser_IdWithModelAndDataset(
                AUDIT_ID,
                USER_ID
        ))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                assembler.assemble(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditNotFoundException.class);
    }

    private void givenAudit() {
        given(auditRepository.findByIdAndUser_IdWithModelAndDataset(
                AUDIT_ID,
                USER_ID
        ))
                .willReturn(Optional.of(audit));

        given(audit.getId()).willReturn(AUDIT_ID);
        given(audit.getAuditName()).willReturn("테스트 감사");
        given(audit.getModel()).willReturn(model);
        given(model.getModelName()).willReturn("credit_model");
    }

    private void givenEmptyMappings() {
        given(regulationMappingQueryService.getMappings(AUDIT_ID))
                .willReturn(List.of());
    }

    private void givenEmptySelfCheck() {
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of());
    }

    private void givenEmptyFairness() {
        given(fairnessResultRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of());
    }

    private void givenEmptyXai() {
        given(xaiResultRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of());
    }

    private AuditRegulationComplianceView mapping(
            String lawName,
            String articleNo,
            ComplianceStatus compliance
    ) {
        return new AuditRegulationComplianceView(
                1L,
                articleNo,
                lawName,
                "조항 본문",
                "요지",
                compliance,
                "자율점검 기반 자동 매칭",
                List.of()
        );
    }

    private SelfCheckAnswerEntity answer(
            SelfCheckItemCode itemCode,
            SelfCheckAnswerValue value
    ) {
        SelfCheckAnswerEntity entity =
                Mockito.mock(SelfCheckAnswerEntity.class);

        given(entity.isNo()).willReturn(value == SelfCheckAnswerValue.NO);

        if (value == SelfCheckAnswerValue.NO) {
            given(entity.getItemCode()).willReturn(itemCode);
        }

        return entity;
    }

    private FairnessResultEntity fairnessResult(
            FairnessMetricCode metricCode,
            FairnessStatus status
    ) {
        FairnessResultEntity entity =
                Mockito.mock(FairnessResultEntity.class);

        given(entity.getStatus()).willReturn(status);

        if (status != FairnessStatus.PASS) {
            given(entity.getAttribute()).willReturn("CODE_GENDER");
            given(entity.getMetricCode()).willReturn(metricCode);
            given(entity.getValue()).willReturn(new BigDecimal("0.2800"));
            given(entity.getThreshold()).willReturn(new BigDecimal("0.2000"));
        }

        return entity;
    }

    private XaiResultEntity xaiResult(XaiStatus status) {
        XaiResultEntity entity = Mockito.mock(XaiResultEntity.class);

        given(entity.getStatus()).willReturn(status);

        if (status != XaiStatus.PASS) {
            given(entity.getMetricCode())
                    .willReturn(XaiMetricCode.values()[0]);
            given(entity.getValue()).willReturn(new BigDecimal("0.6100"));
            given(entity.getThreshold()).willReturn(new BigDecimal("0.7000"));
        }

        return entity;
    }
}
