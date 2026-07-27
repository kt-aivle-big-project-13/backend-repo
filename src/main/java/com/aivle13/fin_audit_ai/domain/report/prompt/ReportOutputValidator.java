package com.aivle13.fin_audit_ai.domain.report.prompt;

import java.util.List;
import java.util.Optional;

public final class ReportOutputValidator {

    private static final List<String> FORBIDDEN_EXPRESSIONS = List.of(
            "100% 안전",
            "완전히 안전",
            "절대 안전",
            "법적 문제가 없다",
            "규제 위험이 없다",
            "위반 가능성이 없다",
            "완벽히 준수",
            "준수를 보장",
            "적법성을 보장"
    );

    private ReportOutputValidator() {
    }

    public static Optional<String> findForbiddenExpression(String content) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }

        return FORBIDDEN_EXPRESSIONS.stream()
                .filter(content::contains)
                .findFirst();
    }

    public static boolean containsForbiddenExpression(String content) {
        return findForbiddenExpression(content).isPresent();
    }

    public static void validate(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "생성된 보고서 내용이 비어 있습니다."
            );
        }

        findForbiddenExpression(content).ifPresent(expression -> {
            throw new IllegalArgumentException(
                    "보고서에 허용되지 않은 판단성 표현이 포함되어 있습니다: "
                            + expression
            );
        });
    }
}