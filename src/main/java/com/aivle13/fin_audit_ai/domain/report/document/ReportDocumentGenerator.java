package com.aivle13.fin_audit_ai.domain.report.document;

import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;

public interface ReportDocumentGenerator {

    // LLM이 생성한 보고서 본문을 PDF 또는 WORD 파일로 변환
    GeneratedReportFile generate(
            Long auditId,
            String content,
            ReportFormat format
    );
}