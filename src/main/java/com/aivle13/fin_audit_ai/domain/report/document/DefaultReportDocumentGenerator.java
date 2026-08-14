package com.aivle13.fin_audit_ai.domain.report.document;

import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 최종 감사 보고서 본문을 PDF·WORD 문서로 만든다.
 *
 * <p>LLM 본문은 마크다운이므로 그대로 찍으면 {@code ##}, {@code |---|} 같은 기호가 문서에
 * 노출된다. {@link ReportMarkdownParser} 로 블록을 뽑아 두 포맷에 같은 구조로 옮긴다.
 */
@Component
public class DefaultReportDocumentGenerator
        implements ReportDocumentGenerator {

    private static final float PDF_BODY_FONT_SIZE = 10f;
    private static final float PDF_LINE_SPACING = 1.6f;
    private static final float PDF_MARGIN = 50f;
    private static final float PDF_BLOCK_GAP = 8f;
    private static final float PDF_CELL_PADDING = 4f;
    private static final float PDF_BULLET_INDENT = 14f;

    // 한글 폰트 자산이 Regular 하나뿐이라 제목은 굵기 대신 크기로 단계를 구분한다.
    private static final Map<Integer, Float> PDF_HEADING_FONT_SIZE = Map.of(
            1, 16f,
            2, 13f,
            3, 11.5f
    );
    private static final float PDF_HEADING_FALLBACK_FONT_SIZE = 10.5f;

    private static final String WORD_FONT_FAMILY = "맑은 고딕";
    private static final int WORD_BODY_FONT_SIZE = 10;
    private static final Map<Integer, Integer> WORD_HEADING_FONT_SIZE = Map.of(
            1, 16,
            2, 13,
            3, 12
    );
    private static final int WORD_HEADING_FALLBACK_FONT_SIZE = 11;
    private static final int WORD_BULLET_INDENT_TWIP = 360;

    @Override
    public GeneratedReportFile generate(
            Long auditId,
            String content,
            ReportFormat format
    ) {
        List<ReportBlock> blocks = ReportMarkdownParser.parse(content);

        return switch (format) {
            case PDF -> generatePdf(auditId, blocks);
            case WORD -> generateWord(auditId, blocks);
            case HTML -> throw new IllegalArgumentException(
                    "HTML 보고서는 AI 서버에서 생성해야 합니다."
            );
        };
    }

    // ---------------------------------------------------------------- PDF

    private GeneratedReportFile generatePdf(
            Long auditId,
            List<ReportBlock> blocks
    ) {
        String fileName = "final-audit-report-%d.pdf".formatted(auditId);

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                InputStream fontStream = getClass().getResourceAsStream(
                        "/fonts/NotoSansKR-Regular.ttf"
                )
        ) {
            if (fontStream == null) {
                throw new IllegalStateException(
                        "PDF 생성에 필요한 한글 폰트를 찾을 수 없습니다."
                );
            }

            PDType0Font font = PDType0Font.load(document, fontStream);
            PdfCanvas canvas = new PdfCanvas(document, font);

            try {
                canvas.write(new ReportBlock.Heading(1, "최종보고서"));
                for (ReportBlock block : blocks) {
                    canvas.write(block);
                }
            } finally {
                canvas.finish();
            }

            document.save(outputStream);

            return new GeneratedReportFile(
                    outputStream.toByteArray(),
                    fileName,
                    "application/pdf"
            );
        } catch (IOException e) {
            throw new IllegalStateException("PDF 보고서 생성에 실패했습니다.", e);
        }
    }

    /**
     * PDF 한 장씩 채워 나가는 커서.
     *
     * <p>표 괘선을 그리려면 텍스트 블록 밖에서 선을 그어야 하므로, 줄마다
     * {@code beginText}/{@code endText} 를 닫아 텍스트와 선 그리기를 섞을 수 있게 한다.
     */
    private static final class PdfCanvas {

        private final PDDocument document;
        private final PDFont font;
        private final float contentWidth;

        // 폰트가 표현하지 못하는 글자는 showText 에서 예외가 나므로 한 번 확인하고 재사용한다.
        private final Map<Character, Boolean> renderableCache = new HashMap<>();

        private PDPage page;
        private PDPageContentStream stream;
        private float cursorY;

        private PdfCanvas(PDDocument document, PDFont font) throws IOException {
            this.document = document;
            this.font = font;
            this.contentWidth = PDRectangle.A4.getWidth() - (PDF_MARGIN * 2);

            newPage();
        }

        private void write(ReportBlock block) throws IOException {
            switch (block) {
                case ReportBlock.Heading heading -> writeHeading(heading);
                case ReportBlock.Paragraph paragraph -> writeParagraph(paragraph);
                case ReportBlock.BulletList bulletList -> writeBullets(bulletList);
                case ReportBlock.Table table -> writeTable(table);
            }
        }

        private void writeHeading(ReportBlock.Heading heading) throws IOException {
            float fontSize = PDF_HEADING_FONT_SIZE.getOrDefault(
                    heading.level(),
                    PDF_HEADING_FALLBACK_FONT_SIZE
            );

            cursorY -= PDF_BLOCK_GAP;

            for (String line : wrap(heading.text(), fontSize, contentWidth)) {
                drawLine(line, fontSize, PDF_MARGIN);
            }

            cursorY -= PDF_BLOCK_GAP / 2;
        }

        private void writeParagraph(ReportBlock.Paragraph paragraph) throws IOException {
            for (String line
                    : wrap(paragraph.text(), PDF_BODY_FONT_SIZE, contentWidth)) {
                drawLine(line, PDF_BODY_FONT_SIZE, PDF_MARGIN);
            }

            cursorY -= PDF_BLOCK_GAP;
        }

        private void writeBullets(ReportBlock.BulletList bulletList) throws IOException {
            for (String item : bulletList.items()) {
                List<String> lines = wrap(
                        item,
                        PDF_BODY_FONT_SIZE,
                        contentWidth - PDF_BULLET_INDENT
                );

                for (int index = 0; index < lines.size(); index++) {
                    // 첫 줄에만 글머리 기호를 붙이고 이어지는 줄은 들여쓰기만 맞춘다.
                    String text = index == 0 ? "• " + lines.get(index) : lines.get(index);
                    float x = index == 0
                            ? PDF_MARGIN
                            : PDF_MARGIN + PDF_BULLET_INDENT;

                    drawLine(text, PDF_BODY_FONT_SIZE, x);
                }
            }

            cursorY -= PDF_BLOCK_GAP;
        }

        private void writeTable(ReportBlock.Table table) throws IOException {
            int columnCount = table.headers().size();

            if (columnCount == 0) {
                return;
            }

            float columnWidth = contentWidth / columnCount;

            drawTableRow(table.headers(), columnWidth, true);

            for (List<String> row : table.rows()) {
                drawTableRow(row, columnWidth, false);
            }

            cursorY -= PDF_BLOCK_GAP;
        }

        private void drawTableRow(
                List<String> cells,
                float columnWidth,
                boolean header
        ) throws IOException {
            float textWidth = columnWidth - (PDF_CELL_PADDING * 2);

            List<List<String>> wrappedCells = new ArrayList<>();
            int maxLines = 1;

            for (String cell : cells) {
                List<String> lines = wrap(cell, PDF_BODY_FONT_SIZE, textWidth);
                wrappedCells.add(lines);
                maxLines = Math.max(maxLines, lines.size());
            }

            float lineHeight = PDF_BODY_FONT_SIZE * PDF_LINE_SPACING;
            float rowHeight = (maxLines * lineHeight) + (PDF_CELL_PADDING * 2);

            // 행이 페이지 경계에 걸치면 통째로 다음 장으로 넘겨 셀이 잘리지 않게 한다.
            if (cursorY - rowHeight < PDF_MARGIN) {
                newPage();
            }

            float top = cursorY;
            float bottom = top - rowHeight;

            if (header) {
                stream.setNonStrokingColor(0.93f, 0.94f, 0.96f);
                stream.addRect(PDF_MARGIN, bottom, columnWidth * cells.size(), rowHeight);
                stream.fill();
                stream.setNonStrokingColor(0f, 0f, 0f);
            }

            for (int column = 0; column < wrappedCells.size(); column++) {
                float x = PDF_MARGIN + (columnWidth * column) + PDF_CELL_PADDING;
                float y = top - PDF_CELL_PADDING;

                for (String line : wrappedCells.get(column)) {
                    y -= lineHeight;
                    drawTextAt(line, PDF_BODY_FONT_SIZE, x, y + (lineHeight * 0.25f));
                }
            }

            drawGrid(top, bottom, columnWidth, cells.size());

            cursorY = bottom;
        }

        private void drawGrid(
                float top,
                float bottom,
                float columnWidth,
                int columnCount
        ) throws IOException {
            float right = PDF_MARGIN + (columnWidth * columnCount);

            stream.setLineWidth(0.5f);
            stream.setStrokingColor(0.6f, 0.6f, 0.6f);

            stream.moveTo(PDF_MARGIN, top);
            stream.lineTo(right, top);
            stream.moveTo(PDF_MARGIN, bottom);
            stream.lineTo(right, bottom);

            for (int column = 0; column <= columnCount; column++) {
                float x = PDF_MARGIN + (columnWidth * column);
                stream.moveTo(x, top);
                stream.lineTo(x, bottom);
            }

            stream.stroke();
            stream.setStrokingColor(0f, 0f, 0f);
        }

        private void drawLine(String text, float fontSize, float x) throws IOException {
            float lineHeight = fontSize * PDF_LINE_SPACING;

            if (cursorY - lineHeight < PDF_MARGIN) {
                newPage();
            }

            cursorY -= lineHeight;

            if (!text.isBlank()) {
                drawTextAt(text, fontSize, x, cursorY);
            }
        }

        private void drawTextAt(
                String text,
                float fontSize,
                float x,
                float y
        ) throws IOException {
            if (text.isEmpty()) {
                return;
            }

            stream.beginText();
            stream.setFont(font, fontSize);
            stream.newLineAtOffset(x, y);
            stream.showText(text);
            stream.endText();
        }

        private void newPage() throws IOException {
            closeStream();

            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            cursorY = page.getMediaBox().getHeight() - PDF_MARGIN;
        }

        private void finish() throws IOException {
            closeStream();
        }

        private void closeStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        /** 폰트 실제 너비를 기준으로 줄을 나눈다. 공백 없는 긴 토큰은 글자 단위로 자른다. */
        private List<String> wrap(String text, float fontSize, float maxWidth)
                throws IOException {
            List<String> lines = new ArrayList<>();
            String sanitized = sanitize(text);

            if (sanitized.isEmpty()) {
                lines.add("");
                return lines;
            }

            StringBuilder current = new StringBuilder();

            for (String word : sanitized.split("\\s+")) {
                if (word.isEmpty()) {
                    continue;
                }

                String candidate = current.isEmpty() ? word : current + " " + word;

                if (widthOf(candidate, fontSize) <= maxWidth) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }

                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }

                if (widthOf(word, fontSize) <= maxWidth) {
                    current.append(word);
                } else {
                    wrapLongWord(word, fontSize, maxWidth, lines, current);
                }
            }

            if (!current.isEmpty()) {
                lines.add(current.toString());
            }

            if (lines.isEmpty()) {
                lines.add("");
            }

            return lines;
        }

        private void wrapLongWord(
                String word,
                float fontSize,
                float maxWidth,
                List<String> lines,
                StringBuilder current
        ) throws IOException {
            for (int index = 0; index < word.length(); index++) {
                char character = word.charAt(index);
                String candidate = current.toString() + character;

                if (widthOf(candidate, fontSize) > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }

                current.append(character);
            }
        }

        private float widthOf(String text, float fontSize) throws IOException {
            return font.getStringWidth(text) / 1000f * fontSize;
        }

        /** 폰트가 표현하지 못하는 글자를 미리 걷어내 showText 실패를 막는다. */
        private String sanitize(String text) {
            StringBuilder sanitized = new StringBuilder(text.length());

            for (char character : text.toCharArray()) {
                if (character == '\t') {
                    sanitized.append(' ');
                    continue;
                }

                if (Character.isISOControl(character)) {
                    continue;
                }

                if (isRenderable(character)) {
                    sanitized.append(character);
                }
            }

            return sanitized.toString().strip();
        }

        private boolean isRenderable(char character) {
            return renderableCache.computeIfAbsent(character, key -> {
                try {
                    font.getStringWidth(String.valueOf(key));
                    return true;
                } catch (IOException | IllegalArgumentException exception) {
                    return false;
                }
            });
        }
    }

    // --------------------------------------------------------------- WORD

    private GeneratedReportFile generateWord(
            Long auditId,
            List<ReportBlock> blocks
    ) {
        String fileName = "final-audit-report-%d.docx".formatted(auditId);

        try (
                XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            writeWordHeading(document, new ReportBlock.Heading(1, "최종보고서"));

            for (ReportBlock block : blocks) {
                switch (block) {
                    case ReportBlock.Heading heading ->
                            writeWordHeading(document, heading);
                    case ReportBlock.Paragraph paragraph ->
                            writeWordParagraph(document, paragraph);
                    case ReportBlock.BulletList bulletList ->
                            writeWordBullets(document, bulletList);
                    case ReportBlock.Table table ->
                            writeWordTable(document, table);
                }
            }

            document.write(outputStream);

            return new GeneratedReportFile(
                    outputStream.toByteArray(),
                    fileName,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );
        } catch (IOException e) {
            throw new IllegalStateException("WORD 보고서 생성에 실패했습니다.", e);
        }
    }

    private void writeWordHeading(
            XWPFDocument document,
            ReportBlock.Heading heading
    ) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(160);
        paragraph.setSpacingAfter(60);

        XWPFRun run = createRun(paragraph);
        run.setBold(true);
        run.setFontSize(WORD_HEADING_FONT_SIZE.getOrDefault(
                heading.level(),
                WORD_HEADING_FALLBACK_FONT_SIZE
        ));
        run.setText(heading.text());
    }

    private void writeWordParagraph(
            XWPFDocument document,
            ReportBlock.Paragraph paragraph
    ) {
        XWPFParagraph target = document.createParagraph();
        target.setSpacingAfter(60);

        XWPFRun run = createRun(target);
        run.setText(paragraph.text());
    }

    private void writeWordBullets(
            XWPFDocument document,
            ReportBlock.BulletList bulletList
    ) {
        for (String item : bulletList.items()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setIndentationLeft(WORD_BULLET_INDENT_TWIP);
            paragraph.setSpacingAfter(20);

            XWPFRun run = createRun(paragraph);
            run.setText("• " + item);
        }
    }

    private void writeWordTable(XWPFDocument document, ReportBlock.Table table) {
        int columnCount = table.headers().size();

        if (columnCount == 0) {
            return;
        }

        int rowCount = table.rows().size() + 1;
        XWPFTable wordTable = document.createTable(rowCount, columnCount);

        fillWordRow(wordTable, 0, table.headers(), true);

        for (int index = 0; index < table.rows().size(); index++) {
            fillWordRow(wordTable, index + 1, table.rows().get(index), false);
        }

        // 표 바로 뒤 문단이 표에 붙어 보이지 않도록 여백 문단을 둔다.
        document.createParagraph().setSpacingAfter(60);
    }

    private void fillWordRow(
            XWPFTable table,
            int rowIndex,
            List<String> cells,
            boolean header
    ) {
        for (int column = 0; column < cells.size(); column++) {
            XWPFTableCell cell = table.getRow(rowIndex).getCell(column);

            // createTable 이 만든 기본 문단을 재사용해 빈 줄이 남지 않게 한다.
            XWPFParagraph paragraph = cell.getParagraphs().isEmpty()
                    ? cell.addParagraph()
                    : cell.getParagraphs().get(0);

            XWPFRun run = createRun(paragraph);
            run.setBold(header);
            run.setText(cells.get(column));
        }
    }

    private XWPFRun createRun(XWPFParagraph paragraph) {
        XWPFRun run = paragraph.createRun();
        run.setFontFamily(WORD_FONT_FAMILY);
        run.setFontSize(WORD_BODY_FONT_SIZE);

        return run;
    }
}
