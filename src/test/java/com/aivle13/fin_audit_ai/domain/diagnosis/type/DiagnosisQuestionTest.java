package com.aivle13.fin_audit_ai.domain.diagnosis.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosisQuestionTest {

    @Test
    void containsAllPreDiagnosisQuestionsInOrder() {
        assertThat(DiagnosisQuestion.values())
                .extracting(DiagnosisQuestion::getCode)
                .containsExactly(
                        "GATE_01",
                        "GATE_02",
                        "A_01",
                        "A_02",
                        "A_03",
                        "B_01",
                        "B_02",
                        "B_03"
                );
    }

    @Test
    void definesExpectedStageGroupAndWeight() {
        assertThat(DiagnosisQuestion.GATE_01.getStage())
                .isEqualTo("QUALITATIVE");
        assertThat(DiagnosisQuestion.GATE_01.getGroup())
                .isEqualTo("GATE");
        assertThat(DiagnosisQuestion.GATE_01.getWeight())
                .isZero();

        assertThat(DiagnosisQuestion.A_01.getStage())
                .isEqualTo("QUANTITATIVE");
        assertThat(DiagnosisQuestion.A_01.getGroup())
                .isEqualTo("A");
        assertThat(DiagnosisQuestion.A_01.getWeight())
                .isEqualTo(2);

        assertThat(DiagnosisQuestion.B_01.getStage())
                .isEqualTo("QUANTITATIVE");
        assertThat(DiagnosisQuestion.B_01.getGroup())
                .isEqualTo("B");
        assertThat(DiagnosisQuestion.B_01.getWeight())
                .isEqualTo(1);
    }

    @Test
    void providesNonBlankQuestionTextForEveryQuestion() {
        assertThat(DiagnosisQuestion.values())
                .allSatisfy(question ->
                        assertThat(question.getQuestionText())
                                .isNotBlank()
                );
    }

    @Test
    void findsQuestionByStoredCode() {
        assertThat(DiagnosisQuestion.findByCode("A_02"))
                .contains(DiagnosisQuestion.A_02);

        assertThat(DiagnosisQuestion.findByCode("UNKNOWN"))
                .isEmpty();
    }
}