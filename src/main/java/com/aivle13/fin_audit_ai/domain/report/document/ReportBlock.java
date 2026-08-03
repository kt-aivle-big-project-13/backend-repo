package com.aivle13.fin_audit_ai.domain.report.document;

import java.util.List;

/**
 * 보고서 본문을 문서로 옮기기 위한 최소 단위.
 *
 * <p>LLM 이 만든 마크다운을 PDF·WORD 어느 쪽으로도 같은 구조로 렌더링하려고 중간 표현을 둔다.
 * 프롬프트가 허용한 문법(제목·문단·목록·표)만 표현하며, 그 밖의 문법은 파서가 문단으로 떨어뜨린다.
 */
public sealed interface ReportBlock {

    /** {@code #} 개수를 단계로 갖는 제목. 단계는 1부터 시작한다. */
    record Heading(int level, String text) implements ReportBlock {
    }

    /** 이어진 줄을 하나로 합친 서술 문단. */
    record Paragraph(String text) implements ReportBlock {
    }

    /** 연속된 {@code -} 항목을 묶은 목록. */
    record BulletList(List<String> items) implements ReportBlock {
    }

    /**
     * 헤더 한 줄과 본문 행으로 이루어진 표.
     *
     * <p>행마다 칸 수가 어긋난 표를 LLM 이 만들 수 있으므로, 파서가 헤더 칸 수에 맞춰
     * 모자란 칸은 빈 문자열로 채우고 넘치는 칸은 버린 뒤에 만든다.
     */
    record Table(List<String> headers, List<List<String>> rows)
            implements ReportBlock {
    }
}
