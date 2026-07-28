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

    // 단정 또는 보장을 직접 부인하는 문맥
    private static final List<Pattern> NON_ASSERTIVE_PATTERNS = List.of(
            Pattern.compile("단정하지(?:는)?\\s*않"),
            Pattern.compile("보장하지(?:는)?\\s*않"),
            Pattern.compile("보장할\\s*수\\s*없")
    );

    // 한 문장 안에 단정 표현과 비단정 표현이 함께 있어도 서로 다른 절이라면 금지 표현을 우회하지 못하도록 절 단위로 분리한다.
    private static final Pattern CLAUSE_BOUNDARY_PATTERN =
            Pattern.compile(
                    "\\s*(?:,|;|하지만|그러나|다만|반면|반대로)\\s*"
            );

    private ReportOutputValidator() {
    }

    public static Optional<String> findForbiddenExpression(
            String content
    ) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalize(content);

        for (String sentence
                : normalized.split("(?<=[.!?。！？])|\\R")) {

            Optional<String> forbiddenExpression =
                    findForbiddenExpressionInSentence(sentence);

            if (forbiddenExpression.isPresent()) {
                return forbiddenExpression;
            }
        }

        return Optional.empty();
    }

    // 문장을 절 단위로 나누고, 금지 표현이 포함된 절에서 직접적인 부정 표현이 있는지 확인한다.
    private static Optional<String> findForbiddenExpressionInSentence(
            String sentence
    ) {
        String[] clauses =
                CLAUSE_BOUNDARY_PATTERN.split(sentence);

        for (String clause : clauses) {
            if (clause.isBlank()) {
                continue;
            }

            Optional<String> forbiddenExpression =
                    findForbiddenExpressionInClause(clause);

            if (forbiddenExpression.isPresent()) {
                return forbiddenExpression;
            }
        }

        return Optional.empty();
    }

    // 같은 절 안에서 비단정 문맥이 확인되는 경우에만 금지 표현을 직접 부정한 것으로 판단한다.
    private static Optional<String> findForbiddenExpressionInClause(
            String clause
    ) {
        for (Pattern pattern : FORBIDDEN_PATTERNS) {
            Matcher matcher = pattern.matcher(clause);

            while (matcher.find()) {
                if (!isNonAssertive(clause)) {
                    return Optional.of(matcher.group());
                }
            }
        }

        return Optional.empty();
    }

    // 금지 표현 포함 여부 확인
    public static boolean containsForbiddenExpression(
            String content
    ) {
        return findForbiddenExpression(content).isPresent();
    }

    // 빈 응답 및 금지 표현 검증
    public static void validate(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "생성된 보고서 내용이 비어 있습니다."
            );
        }

        findForbiddenExpression(content)
                .ifPresent(expression -> {
                    throw new IllegalArgumentException(
                            "보고서에 허용되지 않은 판단성 표현이 포함되어 있습니다: "
                                    + expression
                    );
                });
    }

    // 현재 절에서 단정 또는 보장을 직접 부인하는지 확인
    private static boolean isNonAssertive(String clause) {
        return NON_ASSERTIVE_PATTERNS.stream()
                .anyMatch(pattern ->
                        pattern.matcher(clause).find()
                );
    }

    // 유니코드 및 공백 형태 정규화
    private static String normalize(String content) {
        return Normalizer
                .normalize(content, Normalizer.Form.NFKC)
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .trim();
    }
}