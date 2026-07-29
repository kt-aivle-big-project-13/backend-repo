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
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultReportDocumentGenerator
        implements ReportDocumentGenerator {

    private static final float PDF_FONT_SIZE = 10f;
    private static final float PDF_LINE_HEIGHT = 16f;
    private static final float PDF_MARGIN = 50f;

    @Override
    public GeneratedReportFile generate(
            Long auditId,
            String content,
            ReportFormat format
    ) {
        return switch (format) {
            case PDF -> generatePdf(auditId, content);
            case WORD -> generateWord(auditId, content);
            case HTML -> throw new IllegalArgumentException(
                    "HTML 보고서는 AI 서버에서 생성해야 합니다."
            );
        };
    }

    // 보고서 본문을 PDF 파일로 변환
    private GeneratedReportFile generatePdf(
            Long auditId,
            String content
    ) {
        String fileName =
                "final-audit-report-%d.pdf".formatted(auditId);

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream();
                InputStream fontStream =
                        getClass().getResourceAsStream(
                                "/fonts/NotoSansKR-Regular.ttf"
                        )
        ) {
            if (fontStream == null) {
                throw new IllegalStateException(
                        "PDF 생성에 필요한 한글 폰트를 찾을 수 없습니다."
                );
            }

            PDType0Font font = PDType0Font.load(
                    document,
                    fontStream
            );

            float maxTextWidth =
                    PDRectangle.A4.getWidth() - (PDF_MARGIN * 2);

            List<String> wrappedLines = wrapText(
                    content,
                    font,
                    PDF_FONT_SIZE,
                    maxTextWidth
            );

            writePdfContent(
                    document,
                    font,
                    wrappedLines
            );

            document.save(outputStream);

            return new GeneratedReportFile(
                    outputStream.toByteArray(),
                    fileName,
                    "application/pdf"
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "PDF 보고서 생성에 실패했습니다.",
                    e
            );
        }
    }

    /*
     * 사용 중인 PDF 폰트의 실제 문자열 너비를 기준으로 줄바꿈하고,
     * 페이지 하단 여백에 도달하면 새 페이지를 생성해 본문이 페이지 밖으로 잘리지 않도록 처리한다.
     */
    private void writePdfContent(
            PDDocument document,
            PDFont font,
            List<String> lines
    ) throws IOException {
        PDPage page = createPage(document);
        PDPageContentStream contentStream =
                createContentStream(document, page, font);

        float currentY =
                page.getMediaBox().getHeight() - PDF_MARGIN;

        try {
            for (String line : lines) {
                if (currentY - PDF_LINE_HEIGHT < PDF_MARGIN) {
                    closeContentStream(contentStream);

                    page = createPage(document);

                    contentStream = createContentStream(
                            document,
                            page,
                            font
                    );

                    currentY =
                            page.getMediaBox().getHeight()
                                    - PDF_MARGIN;
                }

                if (!line.isEmpty()) {
                    contentStream.showText(line);
                }

                contentStream.newLine();
                currentY -= PDF_LINE_HEIGHT;
            }
        } finally {
            closeContentStream(contentStream);
        }
    }

    // 새 A4 페이지 생성
    private PDPage createPage(PDDocument document) {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        return page;
    }

    // PDF 페이지 본문 작성을 위한 ContentStream 생성
    private PDPageContentStream createContentStream(
            PDDocument document,
            PDPage page,
            PDFont font
    ) throws IOException {
        PDPageContentStream contentStream =
                new PDPageContentStream(document, page);

        contentStream.beginText();
        contentStream.setFont(font, PDF_FONT_SIZE);
        contentStream.setLeading(PDF_LINE_HEIGHT);
        contentStream.newLineAtOffset(
                PDF_MARGIN,
                page.getMediaBox().getHeight() - PDF_MARGIN
        );

        return contentStream;
    }

    // ContentStream 안전하게 종료
    private void closeContentStream(
            PDPageContentStream contentStream
    ) throws IOException {
        if (contentStream == null) {
            return;
        }

        contentStream.endText();
        contentStream.close();
    }

    // PDF 가로 폭에 맞게 전체 본문 줄바꿈
    private List<String> wrapText(
            String content,
            PDFont font,
            float fontSize,
            float maxWidth
    ) throws IOException {
        List<String> wrappedLines = new ArrayList<>();

        for (String paragraph : content.split("\\R", -1)) {
            if (paragraph.isBlank()) {
                wrappedLines.add("");
                continue;
            }

            wrapParagraph(
                    paragraph,
                    font,
                    fontSize,
                    maxWidth,
                    wrappedLines
            );
        }

        return wrappedLines;
    }

    // 문단 하나를 단어 단위로 줄바꿈
    private void wrapParagraph(
            String paragraph,
            PDFont font,
            float fontSize,
            float maxWidth,
            List<String> wrappedLines
    ) throws IOException {
        StringBuilder currentLine = new StringBuilder();

        for (String word : paragraph.split("\\s+")) {
            String candidate = currentLine.isEmpty()
                    ? word
                    : currentLine + " " + word;

            if (getTextWidth(candidate, font, fontSize)
                    <= maxWidth) {
                currentLine.setLength(0);
                currentLine.append(candidate);
                continue;
            }

            if (!currentLine.isEmpty()) {
                wrappedLines.add(currentLine.toString());
                currentLine.setLength(0);
            }

            if (getTextWidth(word, font, fontSize)
                    <= maxWidth) {
                currentLine.append(word);
            } else {
                wrapLongWord(
                        word,
                        font,
                        fontSize,
                        maxWidth,
                        wrappedLines,
                        currentLine
                );
            }
        }

        if (!currentLine.isEmpty()) {
            wrappedLines.add(currentLine.toString());
        }
    }

    // 공백 없이 긴 문자열은 문자 단위로 줄바꿈
    private void wrapLongWord(
            String word,
            PDFont font,
            float fontSize,
            float maxWidth,
            List<String> wrappedLines,
            StringBuilder currentLine
    ) throws IOException {
        for (int i = 0; i < word.length(); i++) {
            char character = word.charAt(i);

            String candidate =
                    currentLine.toString() + character;

            if (getTextWidth(candidate, font, fontSize)
                    > maxWidth
                    && !currentLine.isEmpty()) {
                wrappedLines.add(currentLine.toString());
                currentLine.setLength(0);
            }

            currentLine.append(character);
        }
    }

    // 사용 중인 PDF 폰트 기준 문자열 너비 계산
    private float getTextWidth(
            String text,
            PDFont font,
            float fontSize
    ) throws IOException {
        return font.getStringWidth(text)
                / 1000f
                * fontSize;
    }

    // 보고서 본문을 WORD 파일로 변환
    private GeneratedReportFile generateWord(
            Long auditId,
            String content
    ) {
        String fileName =
                "final-audit-report-%d.docx".formatted(auditId);

        try (
                XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {
            for (String line : content.split("\\R", -1)) {
                XWPFParagraph paragraph =
                        document.createParagraph();

                XWPFRun run = paragraph.createRun();
                run.setFontFamily("맑은 고딕");
                run.setFontSize(10);
                run.setText(line);
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
}
