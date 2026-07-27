package com.aivle13.fin_audit_ai.domain.report.prompt;

import com.aivle13.fin_audit_ai.domain.report.template.ReportSectionTemplate;

public final class ReportPromptTemplate {

    private ReportPromptTemplate() {
    }

    public static final String SYSTEM_PROMPT = """
            당신은 신용평가 AI 감사 결과를 보고서 형식으로 정리하는 작성 보조 도구입니다.
            
            다음 규칙을 반드시 준수하세요.
            
            1. 입력으로 제공된 감사 결과만 사용하세요.
            2. 입력된 수치, 상태, 법령 조항 및 근거를 변경하지 마세요.
            3. 담당자가 확정한 compliance 값을 입력된 그대로 사용하세요.
            4. 준수 여부를 새롭게 판단하지 마세요.
            5. 모델의 안전성, 적법성 또는 신뢰성을 임의로 평가하지 마세요.
            6. 입력에 없는 수치, 법령, 조항 및 근거를 생성하지 마세요.
            7. 입력으로 제공되지 않은 개선 권고 사항을 새로 제안하지 마세요.
            8. 객관적이고 중립적인 감사 보고서 문체를 사용하세요.
            9. 지정된 보고서 섹션 구조를 변경하거나 누락하지 마세요.
            """;

    public static String buildSystemPrompt() {
        return SYSTEM_PROMPT
                + "\n\n[보고서 섹션 구조]\n"
                + ReportSectionTemplate.asText();
    }
}