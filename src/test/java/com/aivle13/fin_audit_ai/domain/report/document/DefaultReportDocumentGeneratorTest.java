package com.aivle13.fin_audit_ai.domain.report.document;

import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultReportDocumentGeneratorTest {

    private static final String CONTENT = """
            ## 1. 감사 개요
            본 감사는 신용평가 모델을 대상으로 함

            ## 2. 설명가능성 분석 결과
            | 지표 | 값 | 상태 |
            | --- | --- | --- |
            | FIDELITY | 0.92 | 충족 |

            표에 제시된 값은 기준을 상회함

            ## 3. 개선 권고 가이드
            - 민감변수 기여도 모니터링 유지
            - 재감사 주기 설정
            """;

    private final DefaultReportDocumentGenerator generator =
            new DefaultReportDocumentGenerator();

    @Test
    @DisplayName("WORD 본문에 마크다운 기호가 남지 않는다")
    void wordContainsNoMarkdownMarkers() throws IOException {
        GeneratedReportFile file =
                generator.generate(1L, CONTENT, ReportFormat.WORD);

        String text = extractWordText(file.content());

        assertThat(text).doesNotContain("##");
        assertThat(text).doesNotContain("---");
        assertThat(text).doesNotContain("|");
        assertThat(text).contains("1. 감사 개요");
        assertThat(text).contains("본 감사는 신용평가 모델을 대상으로 함");
    }

    @Test
    @DisplayName("WORD 표는 실제 표로 만들어지고 헤더가 굵게 표시된다")
    void wordRendersRealTable() throws IOException {
        GeneratedReportFile file =
                generator.generate(1L, CONTENT, ReportFormat.WORD);

        try (
                XWPFDocument document =
                        new XWPFDocument(new ByteArrayInputStream(file.content()))
        ) {
            List<XWPFTable> tables = document.getTables();

            assertThat(tables).hasSize(1);

            XWPFTable table = tables.get(0);
            assertThat(table.getRows()).hasSize(2);
            assertThat(table.getRow(0).getCell(0).getText()).isEqualTo("지표");
            assertThat(table.getRow(1).getCell(1).getText()).isEqualTo("0.92");

            assertThat(
                    table.getRow(0)
                            .getCell(0)
                            .getParagraphs()
                            .get(0)
                            .getRuns()
                            .get(0)
                            .isBold()
            ).isTrue();
        }
    }

    @Test
    @DisplayName("WORD 목록은 글머리 기호가 붙은 문단으로 만들어진다")
    void wordRendersBulletItems() throws IOException {
        GeneratedReportFile file =
                generator.generate(1L, CONTENT, ReportFormat.WORD);

        String text = extractWordText(file.content());

        assertThat(text).contains("• 민감변수 기여도 모니터링 유지");
        assertThat(text).contains("• 재감사 주기 설정");
        assertThat(text).doesNotContain("- 민감변수");
    }

    @Test
    @DisplayName("PDF 본문에 마크다운 기호가 남지 않고 표 내용이 들어간다")
    void pdfContainsNoMarkdownMarkers() throws IOException {
        GeneratedReportFile file =
                generator.generate(1L, CONTENT, ReportFormat.PDF);

        try (PDDocument document = Loader.loadPDF(file.content())) {
            String text = new PDFTextStripper().getText(document);

            assertThat(text).doesNotContain("##");
            assertThat(text).doesNotContain("|");
            assertThat(text).contains("1. 감사 개요");
            assertThat(text).contains("FIDELITY");
            assertThat(text).contains("0.92");
            assertThat(text).contains("충족");
        }
    }

    @Test
    @DisplayName("HTML 포맷은 AI 서버 담당이므로 거부한다")
    void rejectsHtmlFormat() {
        assertThatThrownBy(() ->
                generator.generate(1L, CONTENT, ReportFormat.HTML)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    private String extractWordText(byte[] content) throws IOException {
        try (
                XWPFDocument document =
                        new XWPFDocument(new ByteArrayInputStream(content))
        ) {
            StringBuilder text = new StringBuilder();

            document.getParagraphs()
                    .forEach(paragraph -> text.append(paragraph.getText()).append('\n'));

            document.getTables().forEach(table ->
                    table.getRows().forEach(row ->
                            row.getTableCells().forEach(cell ->
                                    text.append(cell.getText()).append('\n')
                            )
                    )
            );

            return text.toString();
        }
    }
}
