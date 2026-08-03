package com.aivle13.fin_audit_ai.domain.report.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportMarkdownParserTest {

    @Test
    @DisplayName("제목은 # 개수를 단계로 갖는 Heading 으로 파싱된다")
    void parsesHeadingLevels() {
        List<ReportBlock> blocks = ReportMarkdownParser.parse("""
                # 최종 감사 보고서
                ## 1. 감사 개요
                ### 1.1 대상
                """);

        assertThat(blocks).containsExactly(
                new ReportBlock.Heading(1, "최종 감사 보고서"),
                new ReportBlock.Heading(2, "1. 감사 개요"),
                new ReportBlock.Heading(3, "1.1 대상")
        );
    }

    @Test
    @DisplayName("이어진 줄은 한 문단으로 합치고 빈 줄에서 문단을 끊는다")
    void mergesContinuedLinesIntoOneParagraph() {
        List<ReportBlock> blocks = ReportMarkdownParser.parse("""
                첫 문단의 앞부분이며
                뒷부분임

                둘째 문단임
                """);

        assertThat(blocks).containsExactly(
                new ReportBlock.Paragraph("첫 문단의 앞부분이며 뒷부분임"),
                new ReportBlock.Paragraph("둘째 문단임")
        );
    }

    @Test
    @DisplayName("연속된 - 항목은 하나의 목록으로 묶인다")
    void groupsConsecutiveBulletItems() {
        List<ReportBlock> blocks = ReportMarkdownParser.parse("""
                - 첫째 권고
                - 둘째 권고
                """);

        assertThat(blocks).containsExactly(
                new ReportBlock.BulletList(List.of("첫째 권고", "둘째 권고"))
        );
    }

    @Test
    @DisplayName("표는 구분선을 빼고 헤더와 본문 행으로 파싱된다")
    void parsesTableWithoutDelimiterRow() {
        List<ReportBlock> blocks = ReportMarkdownParser.parse("""
                | 지표 | 값 | 상태 |
                | --- | --- | --- |
                | FIDELITY | 0.92 | 충족 |
                | GLOBAL_STABILITY | 0.88 | 충족 |
                """);

        assertThat(blocks).containsExactly(
                new ReportBlock.Table(
                        List.of("지표", "값", "상태"),
                        List.of(
                                List.of("FIDELITY", "0.92", "충족"),
                                List.of("GLOBAL_STABILITY", "0.88", "충족")
                        )
                )
        );
    }

    @Test
    @DisplayName("헤더와 칸 수가 어긋난 행은 헤더 기준으로 길이를 맞춘다")
    void normalizesRowsToHeaderColumnCount() {
        List<ReportBlock> blocks = ReportMarkdownParser.parse("""
                | 지표 | 값 |
                | --- | --- |
                | FIDELITY |
                | GLOBAL_STABILITY | 0.88 | 남는칸 |
                """);

        assertThat(blocks).containsExactly(
                new ReportBlock.Table(
                        List.of("지표", "값"),
                        List.of(
                                List.of("FIDELITY", ""),
                                List.of("GLOBAL_STABILITY", "0.88")
                        )
                )
        );
    }

    @Test
    @DisplayName("금지된 인라인 강조 기호가 섞여 와도 기호만 제거하고 내용은 남긴다")
    void stripsInlineEmphasisMarkers() {
        List<ReportBlock> blocks = ReportMarkdownParser.parse("""
                ## **2. 종합 요약**

                `FIDELITY` 지표는 **0.92** 로 *충족* 임
                """);

        assertThat(blocks).containsExactly(
                new ReportBlock.Heading(2, "2. 종합 요약"),
                new ReportBlock.Paragraph("FIDELITY 지표는 0.92 로 충족 임")
        );
    }

    @Test
    @DisplayName("지표 코드의 언더스코어는 강조 기호로 오인해 지우지 않는다")
    void keepsUnderscoresInMetricCodes() {
        List<ReportBlock> blocks = ReportMarkdownParser.parse("""
                GLOBAL_STABILITY 와 EQUAL_OPPORTUNITY 는 CODE_GENDER 기준으로 평가함
                """);

        assertThat(blocks).containsExactly(
                new ReportBlock.Paragraph(
                        "GLOBAL_STABILITY 와 EQUAL_OPPORTUNITY 는 CODE_GENDER 기준으로 평가함"
                )
        );
    }

    @Test
    @DisplayName("표 뒤에 이어지는 서술은 별도 문단으로 분리된다")
    void separatesParagraphFollowingTable() {
        List<ReportBlock> blocks = ReportMarkdownParser.parse("""
                | 지표 | 값 |
                | --- | --- |
                | FIDELITY | 0.92 |
                표에 제시된 값은 기준을 상회함
                """);

        assertThat(blocks).containsExactly(
                new ReportBlock.Table(
                        List.of("지표", "값"),
                        List.of(List.of("FIDELITY", "0.92"))
                ),
                new ReportBlock.Paragraph("표에 제시된 값은 기준을 상회함")
        );
    }

    @Test
    @DisplayName("비어 있거나 null 인 본문은 빈 목록을 돌려준다")
    void returnsEmptyListForBlankContent() {
        assertThat(ReportMarkdownParser.parse(null)).isEmpty();
        assertThat(ReportMarkdownParser.parse("   ")).isEmpty();
    }
}
