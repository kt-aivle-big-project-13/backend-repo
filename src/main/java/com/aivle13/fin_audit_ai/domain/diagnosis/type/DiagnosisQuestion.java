package com.aivle13.fin_audit_ai.domain.diagnosis.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum DiagnosisQuestion {

    GATE_01(
            "국민의 생명, 신체의 안전 또는 기본권에 "
                    + "중대한 영향을 미칠 수 있나요?",
            "QUALITATIVE",
            "GATE",
            0
    ),
    GATE_02(
            "금융 거래, 신용 평가 등 중요 의사결정에 활용되나요?",
            "QUALITATIVE",
            "GATE",
            0
    ),
    A_01(
            "AI 모델의 복잡도가 기존보다 증가한 신규 모델"
                    + "(블랙박스 등)에 기반한 시스템인가요?",
            "QUANTITATIVE",
            "A",
            2
    ),
    A_02(
            "1만 명 이상의 금융소비자를 대상으로 하는 "
                    + "AI 시스템인가요?",
            "QUANTITATIVE",
            "A",
            2
    ),
    A_03(
            "AI 결과가 사람의 실질적인 개입 없이 "
                    + "최종 의사결정으로 이어지나요?",
            "QUANTITATIVE",
            "A",
            2
    ),
    B_01(
            "대리변수(Proxy Variable)를 자주 활용하는 "
                    + "AI 시스템인가요?",
            "QUANTITATIVE",
            "B",
            1
    ),
    B_02(
            "특정 업무용으로 개발된 AI를 다른 업무에 "
                    + "일시적으로 활용하고 있나요?",
            "QUANTITATIVE",
            "B",
            1
    ),
    B_03(
            "국외 데이터를 주로 학습한 해외 AI 시스템을 "
                    + "활용하고 있나요?",
            "QUANTITATIVE",
            "B",
            1
    );

    private final String questionText;
    private final String stage;
    private final String group;
    private final int weight;

    public String getCode() {
        return name();
    }

    public static Optional<DiagnosisQuestion> findByCode(
            String questionCode
    ) {
        return Arrays.stream(values())
                .filter(question ->
                        question.name().equals(questionCode)
                )
                .findFirst();
    }
}