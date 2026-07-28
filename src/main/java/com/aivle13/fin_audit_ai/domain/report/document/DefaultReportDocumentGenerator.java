package com.aivle13.fin_audit_ai.domain.report.document;

import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class DefaultReportDocumentGenerator
        implements ReportDocumentGenerator {

    @Override
    public GeneratedReportFile generate(
            Long auditId,
            String content,
            ReportFormat format
    ) {
        return switch (format) {
            case PDF -> generatePdf(auditId, content);
            case WORD -> generateWord(auditId, content);
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

            PDPage page = new PDPage();
            document.addPage(page);

            try (
                    PDPageContentStream contentStream =
                            new PDPageContentStream(document, page)
            ) {
                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.setLeading(16);
                contentStream.newLineAtOffset(50, 780);

                for (String line : content.split("\\R")) {
                    contentStream.showText(line);
                    contentStream.newLine();
                }

                contentStream.endText();
            }

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
            for (String line : content.split("\\R")) {
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
            throw new IllegalStateException(
                    "WORD 보고서 생성에 실패했습니다.",
                    e
            );
        }
    }
}