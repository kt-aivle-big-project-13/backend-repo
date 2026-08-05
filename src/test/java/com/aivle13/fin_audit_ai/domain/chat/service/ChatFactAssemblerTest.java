package com.aivle13.fin_audit_ai.domain.chat.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessGroupStatEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessGroupStatRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.ShapFeatureImportanceRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.selfcheck.SelfCheckAnswerValue;
import com.aivle13.fin_audit_ai.domain.audit.type.selfcheck.SelfCheckItemCode;
import com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiStatus;
import com.aivle13.fin_audit_ai.global.ai.dto.chat.request.ChatAnswerRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * 챗봇 근거 조립 시 enum 값이 영어 원문(name())이 아니라 화면과 맞춘 한글 라벨로
 * 나가는지 확인한다. AI 서버는 주어진 근거만 인용해 답하므로, 여기서 한글화하지
 * 않으면 챗봇 답변에 "DEMOGRAPHIC_PARITY", "PASS" 같은 영어가 그대로 노출된다.
 */
@ExtendWith(MockitoExtension.class)
class ChatFactAssemblerTest {

    @Mock
    private FairnessResultRepository fairnessResultRepository;

    @Mock
    private FairnessGroupStatRepository fairnessGroupStatRepository;

    @Mock
    private XaiResultRepository xaiResultRepository;

    @Mock
    private ShapFeatureImportanceRepository shapFeatureImportanceRepository;

    @Mock
    private SelfCheckAnswerRepository selfCheckAnswerRepository;

    @InjectMocks
    private ChatFactAssembler assembler;

    @Test
    void translatesFairnessResultIntoKoreanLabels() {
        FairnessResultEntity result = FairnessResultEntity.of(
                null,
                "AGE_GROUP",
                FairnessMetricCode.DEMOGRAPHIC_PARITY,
                new BigDecimal("0.1195"),
                new BigDecimal("0.2000"),
                FairnessStatus.PASS
        );

        givenOtherSourcesAreEmpty();
        given(fairnessResultRepository.findAllByAudit_Id(anyLong()))
                .willReturn(List.of(result));

        List<ChatAnswerRequest.AuditFact> facts = assembler.assemble(1L);

        assertThat(facts).hasSize(1);
        ChatAnswerRequest.AuditFact fact = facts.get(0);

        assertThat(fact.reference())
                .isEqualTo("인구통계학적 평등성(Demographic Parity) / 연령대");
        assertThat(fact.detail()).isEqualTo("임계값 0.2000, 상태 충족");

        // 영어 enum 원문은 더 이상 그대로 노출되지 않는다.
        assertThat(fact.reference()).doesNotContain("DEMOGRAPHIC_PARITY");
        assertThat(fact.detail()).doesNotContain("PASS");
    }

    @Test
    void translatesXaiResultIntoKoreanLabels() {
        XaiResultEntity result = XaiResultEntity.of(
                null,
                XaiMetricCode.GLOBAL_STABILITY,
                new BigDecimal("0.8100"),
                new BigDecimal("0.7000"),
                XaiStatus.WARNING
        );

        givenOtherSourcesAreEmpty();
        given(xaiResultRepository.findAllByAudit_Id(anyLong()))
                .willReturn(List.of(result));

        List<ChatAnswerRequest.AuditFact> facts = assembler.assemble(1L);

        assertThat(facts).hasSize(1);
        ChatAnswerRequest.AuditFact fact = facts.get(0);

        assertThat(fact.reference()).isEqualTo("설명 일관성");
        assertThat(fact.detail()).isEqualTo("임계값 0.7000, 상태 주의");
    }

    @Test
    void translatesGroupStatAttributeIntoKoreanLabel() {
        FairnessGroupStatEntity stat = FairnessGroupStatEntity.of(
                null, "CODE_GENDER", "여성", 120,
                new BigDecimal("0.6500"), new BigDecimal("0.1200"),
                80, 10, 25, 5, new BigDecimal("0.9000")
        );

        given(fairnessGroupStatRepository.findAllByAudit_Id(anyLong()))
                .willReturn(List.of(stat));
        given(fairnessResultRepository.findAllByAudit_Id(anyLong())).willReturn(List.of());
        given(xaiResultRepository.findAllByAudit_Id(anyLong())).willReturn(List.of());
        given(shapFeatureImportanceRepository.findAllByAudit_IdOrderByRankAsc(anyLong()))
                .willReturn(List.of());
        given(selfCheckAnswerRepository.findAllByAudit_Id(anyLong())).willReturn(List.of());

        List<ChatAnswerRequest.AuditFact> facts = assembler.assemble(1L);

        assertThat(facts).hasSize(1);
        assertThat(facts.get(0).reference()).isEqualTo("성별=여성");
    }

    @Test
    void fallsBackToRawAttributeWhenUnmapped() {
        FairnessResultEntity result = FairnessResultEntity.of(
                null,
                "INCOME_LEVEL",
                FairnessMetricCode.EQUAL_OPPORTUNITY,
                new BigDecimal("0.1000"),
                new BigDecimal("0.2000"),
                FairnessStatus.REVIEW
        );

        given(fairnessResultRepository.findAllByAudit_Id(anyLong()))
                .willReturn(List.of(result));
        givenOtherSourcesAreEmpty();

        List<ChatAnswerRequest.AuditFact> facts = assembler.assemble(1L);

        // 아직 한글 라벨이 없는 보호속성은 예외 없이 원래 값 그대로 나간다.
        assertThat(facts.get(0).reference())
                .isEqualTo("기회의 균등(Equal Opportunity) / INCOME_LEVEL");
    }

    @Test
    void keepsSelfCheckAnswersUnaffected() {
        SelfCheckAnswerEntity answer = SelfCheckAnswerEntity.of(
                null, SelfCheckItemCode.TR_01, SelfCheckAnswerValue.YES
        );

        given(selfCheckAnswerRepository.findAllByAudit_Id(anyLong()))
                .willReturn(List.of(answer));
        given(fairnessResultRepository.findAllByAudit_Id(anyLong())).willReturn(List.of());
        given(fairnessGroupStatRepository.findAllByAudit_Id(anyLong())).willReturn(List.of());
        given(xaiResultRepository.findAllByAudit_Id(anyLong())).willReturn(List.of());
        given(shapFeatureImportanceRepository.findAllByAudit_IdOrderByRankAsc(anyLong()))
                .willReturn(List.of());

        List<ChatAnswerRequest.AuditFact> facts = assembler.assemble(1L);

        assertThat(facts).hasSize(1);
        assertThat(facts.get(0).reference())
                .isEqualTo("자가점검 · AI 심사 사실을 고객에게 사전에 알리고 있나요?");
        assertThat(facts.get(0).value()).isEqualTo("예");
    }

    private void givenOtherSourcesAreEmpty() {
        given(fairnessGroupStatRepository.findAllByAudit_Id(anyLong())).willReturn(List.of());
        given(xaiResultRepository.findAllByAudit_Id(anyLong())).willReturn(List.of());
        given(shapFeatureImportanceRepository.findAllByAudit_IdOrderByRankAsc(anyLong()))
                .willReturn(List.of());
        given(selfCheckAnswerRepository.findAllByAudit_Id(anyLong())).willReturn(List.of());
    }
}