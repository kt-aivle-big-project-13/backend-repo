package com.aivle13.fin_audit_ai.domain.report.prompt;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReportOutputValidator {

    // 과도한 보장 및 단정 표현
    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
            Pattern.compile("100\\s*%\\s*안전"),
            Pattern.compile("완전히\\s*안전"),
            Pattern.compile("절대(?:적으로)?\\s*안전"),
            Pattern.compile("법적\\s*문제(?:가|는)?\\s*없"),
            Pattern.compile("규제\\s*위험(?:이|은)?\\s*없"),
            Pattern.compile("위반\\s*가능성(?:이|은)?\\s*없"),
            Pattern.compile("완벽히\\s*준수"),
            Pattern.compile("준수(?:를)?\\s*보장"),
            Pattern.compile("적법성(?:을)?\\s*보장")
    );

    // 단정 또는 보장을 부인하는 문맥
    private static final List<Pattern> NON_ASSERTIVE_PATTERNS = List.of(
            Pattern.compile("단정하지\\s*않"),
            Pattern.compile("보장하지\\s*않"),
            Pattern.compile("보장할\\s*수\\s*없")
    );

    private ReportOutputValidator() {
    }

    public static Optional<String> findForbiddenExpression(String content) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalize(content);

        for (String sentence : normalized.split("(?<=[.!?。！？])|\\R")) {
            if (isNonAssertive(sentence)) {
                continue;
            }

            for (Pattern pattern : FORBIDDEN_PATTERNS) {
                Matcher matcher = pattern.matcher(sentence);

                if (matcher.find()) {
                    return Optional.of(matcher.group());
                }
            }
        }

        return Optional.empty();
    }

    // 금지 표현 포함 여부 확인
    public static boolean containsForbiddenExpression(String content) {
        return findForbiddenExpression(content).isPresent();
    }

    // 빈 응답 및 금지 표현 검증
    public static void validate(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("생성된 보고서 내용이 비어 있습니다.");
        }

        findForbiddenExpression(content).ifPresent(expression -> {
            throw new IllegalArgumentException(
                    "보고서에 허용되지 않은 판단성 표현이 포함되어 있습니다: "
                            + expression
            );
        });
    }

    // 비단정 문맥 여부 확인
    private static boolean isNonAssertive(String sentence) {
        return NON_ASSERTIVE_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(sentence).find());
    }

    // 유니코드 및 공백 형태 정규화
    private static String normalize(String content) {
        return Normalizer.normalize(content, Normalizer.Form.NFKC)
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .trim();
    }
}