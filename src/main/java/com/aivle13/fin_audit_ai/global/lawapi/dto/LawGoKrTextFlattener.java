package com.aivle13.fin_audit_ai.global.lawapi.dto;

import tools.jackson.databind.JsonNode;

/**
 * law.go.kr 응답의 "조문내용"(간단한 조항은 평문)과 "항"→"호"→"목"(항목이 있는 조항은
 * 트리 구조) 두 형태를 하나의 평문 텍스트로 합친다. 우리 DB의 law_articles.content는
 * 항상 완결된 평문 하나이므로, API 쪽 구조를 그대로 저장하지 않고 여기서 평탄화한다.
 */
public final class LawGoKrTextFlattener {

    private LawGoKrTextFlattener() {
    }

    public static String flatten(String directContent, JsonNode paragraphs) {
        StringBuilder builder = new StringBuilder();

        if (directContent != null && !directContent.isBlank()) {
            builder.append(directContent.strip());
        }

        if (paragraphs != null && !paragraphs.isMissingNode() && !paragraphs.isNull()) {
            collect(paragraphs, builder);
        }

        return builder.toString().strip();
    }

    private static void collect(JsonNode node, StringBuilder builder) {
        if (node.isArray()) {
            for (JsonNode child : node) {
                collect(child, builder);
            }
            return;
        }

        if (!node.isObject()) {
            return;
        }

        appendIfPresent(node, "항내용", builder);
        appendIfPresent(node, "호내용", builder);
        appendIfPresent(node, "목내용", builder);

        if (node.has("호")) {
            collect(node.get("호"), builder);
        }
        if (node.has("목")) {
            collect(node.get("목"), builder);
        }
    }

    private static void appendIfPresent(JsonNode node, String field, StringBuilder builder) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            return;
        }

        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(value.asString());
    }
}
