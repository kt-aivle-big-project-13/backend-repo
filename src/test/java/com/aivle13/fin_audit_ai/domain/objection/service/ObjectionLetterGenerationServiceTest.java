package com.aivle13.fin_audit_ai.domain.objection.service;

import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionModelEvidenceResponse;
import com.aivle13.fin_audit_ai.domain.objection.entity.ObjectionEntity;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionDecision;
import com.aivle13.fin_audit_ai.global.exception.llm.LlmServerErrorException;
import com.aivle13.fin_audit_ai.global.llm.ReportLlmClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObjectionLetterGenerationServiceTest {

    @Mock
    private ReportLlmClient reportLlmClient;

    @InjectMocks
    private ObjectionLetterGenerationService
            objectionLetterGenerationService;

    @Test
    void generatesLetterWithSeparatedCaseAndGlobalEvidence() {
        ObjectionEntity objection = objection();
        ObjectionModelEvidenceResponse evidence =
                new ObjectionModelEvidenceResponse(
                        1,
                        "EXT_SOURCE_2",
                        "외부 평가 지표 2",
                        new BigDecimal("0.136000"),
                        new BigDecimal("0.240000"),
                        "RISK_DECREASE"
                );

        when(reportLlmClient.generate(
                anyString(),
                anyString()
        )).thenReturn("  생성된 고객 안내문  ");

        String result = objectionLetterGenerationService.generate(
                objection,
                ObjectionDecision.REJECT_MAINTAIN,
                List.of(evidence),
                false
        );

        assertThat(result).isEqualTo("생성된 고객 안내문");

        ArgumentCaptor<String> systemPrompt =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPrompt =
                ArgumentCaptor.forClass(String.class);

        verify(reportLlmClient).generate(
                systemPrompt.capture(),
                userPrompt.capture()
        );

        assertThat(systemPrompt.getValue())
                .contains(
                        "전역 SHAP 결과를 해당 고객의 직접적인 거절 원인"
                );

        assertThat(userPrompt.getValue())
                .contains(
                        "[고객 건별 판단 근거 - CSV 기록]",
                        "부채비율 82%",
                        "[모델의 일반적인 판단 경향 - 최신 감사 전역 SHAP]",
                        "외부 평가 지표 2",
                        "전체 감사 데이터"
                );
    }

    @Test
    void regenerationKeepsDecisionAndRequestsDifferentWording() {
        ObjectionEntity objection = objection();

        when(reportLlmClient.generate(
                anyString(),
                anyString()
        )).thenReturn("재생성된 안내문");

        objectionLetterGenerationService.generate(
                objection,
                ObjectionDecision.REEXAMINATION,
                List.of(),
                true
        );

        ArgumentCaptor<String> userPrompt =
                ArgumentCaptor.forClass(String.class);

        verify(reportLlmClient).generate(
                anyString(),
                userPrompt.capture()
        );

        assertThat(userPrompt.getValue())
                .contains(
                        "동일한 사실과 담당자 결정을 유지",
                        "[담당자 결정]",
                        "재심사",
                        "조회된 전역 SHAP 근거 없음"
                );
    }

    @Test
    void rejectsNullDecisionBeforeCallingLlm() {
        ObjectionEntity objection = mock(ObjectionEntity.class);

        assertThatThrownBy(() ->
                objectionLetterGenerationService.generate(
                        objection,
                        null,
                        List.of(),
                        false
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "고객 안내문 생성에는 담당자 결정이 필요합니다."
                );

        verifyNoInteractions(reportLlmClient);
    }

    @Test
    void rejectsBlankLlmResponse() {
        ObjectionEntity objection = objection();

        when(reportLlmClient.generate(
                anyString(),
                anyString()
        )).thenReturn("   ");

        assertThatThrownBy(() ->
                objectionLetterGenerationService.generate(
                        objection,
                        ObjectionDecision.REJECT_MAINTAIN,
                        List.of(),
                        false
                )
        ).isInstanceOf(LlmServerErrorException.class);
    }

    private ObjectionEntity objection() {
        ObjectionEntity objection = mock(ObjectionEntity.class);

        when(objection.getCustomerName()).thenReturn("조영*");
        when(objection.getTitle()).thenReturn("왜 거절됐나요?");
        when(objection.getContent())
                .thenReturn(
                        "대출이 거절된 이유를 확인하고 싶습니다."
                );
        when(objection.getShapEvidence())
                .thenReturn(
                        "부채비율 82%; 최근 연체 이력 2건/6개월"
                );
        when(objection.getStaffNote())
                .thenReturn(
                        "부채비율과 연체 이력을 종합 검토했습니다."
                );

        return objection;
    }
}