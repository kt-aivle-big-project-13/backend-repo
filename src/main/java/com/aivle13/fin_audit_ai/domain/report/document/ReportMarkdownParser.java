package com.aivle13.fin_audit_ai.domain.report.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * LLM 이 만든 보고서 본문(마크다운)을 문서 블록으로 바꾼다.
 *
 * <p>프롬프트가 제목·목록·표만 쓰도록 지시하지만 LLM 출력은 보장되지 않으므로, 허용하지 않은
 * 문법이 섞여 와도 기호가 문서에 그대로 새어 나가지 않게 처리한다. 해석하지 못한 줄은 버리지
 * 않고 문단으로 남겨 내용 자체는 유실되지 않도록 한다.
 */
public final class ReportMarkdownParser {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern BULLET = Pattern.compile("^[-*+]\\s+(.*)$");

    // |---|:---:|---| 처럼 칸마다 하이픈과 정렬 콜론만 있는 표 구분선
    private static final Pattern TABLE_DELIMITER =
            Pattern.compile("^\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?$");

    // 굵게·취소선·인라인 코드 기호. 프롬프트에서 금지하지만 섞여 오면 기호만 걷어낸다.
    // 언더스코어(_)는 건드리지 않는다 — GLOBAL_STABILITY, EQUAL_OPPORTUNITY 처럼
    // 이 보고서에 그대로 실리는 지표 코드가 망가진다.
    private static final Pattern INLINE_EMPHASIS =
            Pattern.compile("(\\*{1,3}|~{2}|`+)");

    private static final int MAX_TABLE_COLUMNS = 12;

    private ReportMarkdownParser() {
    }

    public static List<ReportBlock> parse(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<String> lines = Arrays.asList(content.split("\\R", -1));

        List<ReportBlock> blocks = new ArrayList<>();
        List<String> paragraphBuffer = new ArrayList<>();
        List<String> bulletBuffer = new ArrayList<>();
        List<String> tableBuffer = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.strip();

            if (isTableRow(line)) {
                flushParagraph(paragraphBuffer, blocks);
                flushBullets(bulletBuffer, blocks);
                tableBuffer.add(line);
                continue;
            }

            flushTable(tableBuffer, blocks);

            if (line.isEmpty()) {
                flushParagraph(paragraphBuffer, blocks);
                flushBullets(bulletBuffer, blocks);
                continue;
            }

            var headingMatcher = HEADING.matcher(line);
            if (headingMatcher.matches()) {
                flushParagraph(paragraphBuffer, blocks);
                flushBullets(bulletBuffer, blocks);

                String text = clean(headingMatcher.group(2));
                if (!text.isEmpty()) {
                    blocks.add(new ReportBlock.Heading(
                            headingMatcher.group(1).length(),
                            text
                    ));
                }
                continue;
            }

            var bulletMatcher = BULLET.matcher(line);
            if (bulletMatcher.matches()) {
                flushParagraph(paragraphBuffer, blocks);

                String item = clean(bulletMatcher.group(1));
                if (!item.isEmpty()) {
                    bulletBuffer.add(item);
                }
                continue;
            }

            flushBullets(bulletBuffer, blocks);
            paragraphBuffer.add(clean(line));
        }

        flushTable(tableBuffer, blocks);
        flushParagraph(paragraphBuffer, blocks);
        flushBullets(bulletBuffer, blocks);

        return List.copyOf(blocks);
    }

    private static boolean isTableRow(String line) {
        return line.startsWith("|") && line.length() > 1;
    }

    private static void flushParagraph(
            List<String> buffer,
            List<ReportBlock> blocks
    ) {
        if (buffer.isEmpty()) {
            return;
        }

        // 마크다운은 줄바꿈만으로 문단을 끊지 않으므로, 이어진 줄은 한 문단으로 합친다.
        String text = String.join(" ", buffer).strip();
        buffer.clear();

        if (!text.isEmpty()) {
            blocks.add(new ReportBlock.Paragraph(text));
        }
    }

    private static void flushBullets(
            List<String> buffer,
            List<ReportBlock> blocks
    ) {
        if (buffer.isEmpty()) {
            return;
        }

        blocks.add(new ReportBlock.BulletList(List.copyOf(buffer)));
        buffer.clear();
    }

    private static void flushTable(
            List<String> buffer,
            List<ReportBlock> blocks
    ) {
        if (buffer.isEmpty()) {
            return;
        }

        List<String> rows = List.copyOf(buffer);
        buffer.clear();

        List<List<String>> cellRows = rows.stream()
                .filter(row -> !TABLE_DELIMITER.matcher(row).matches())
                .map(ReportMarkdownParser::splitCells)
                .filter(cells -> !cells.isEmpty())
                .toList();

        if (cellRows.isEmpty()) {
            return;
        }

        List<String> headers = cellRows.get(0);
        int columnCount = Math.min(headers.size(), MAX_TABLE_COLUMNS);

        List<List<String>> bodyRows = cellRows.stream()
                .skip(1)
                .map(cells -> normalizeRow(cells, columnCount))
                .toList();

        blocks.add(new ReportBlock.Table(
                normalizeRow(headers, columnCount),
                bodyRows
        ));
    }

    // 헤더와 칸 수가 어긋난 행이 와도 표가 깨지지 않도록 길이를 맞춘다.
    private static List<String> normalizeRow(List<String> cells, int columnCount) {
        List<String> normalized = new ArrayList<>(columnCount);

        for (int index = 0; index < columnCount; index++) {
            normalized.add(index < cells.size() ? cells.get(index) : "");
        }

        return List.copyOf(normalized);
    }

    private static List<String> splitCells(String row) {
        String trimmed = row.strip();

        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        // 빈 칸도 자리를 지켜야 열이 밀리지 않으므로 limit 을 -1 로 둔다.
        List<String> cells = Arrays.stream(trimmed.split("\\|", -1))
                .map(ReportMarkdownParser::clean)
                .toList();

        return cells.stream().allMatch(String::isEmpty) ? List.of() : cells;
    }

    private static String clean(String text) {
        return INLINE_EMPHASIS.matcher(text)
                .replaceAll("")
                .strip();
    }
}
