package com.aivle13.fin_audit_ai.global.ai.dto.report.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버가 리포트와 함께 돌려주는 섹션별 서술 한 건.
 *
 * <p>리포트 본문은 S3 에 파일로만 있어 그대로 두면 챗봇이 질문마다 파일을 받아 파싱해야 한다.
 * 리포트를 만들 때 이미 생성한 서술을 그대로 받아 저장하므로 추가 LLM 호출은 없다.
 *
 * <p>목차 제목까지 함께 받는 이유는 섹션 키(`metric_results`)와 제목("5. 공정성 지표 결과")의
 * 매핑을 AI 서버가 소유하기 때문이다.
 */
public record ReportNarrativeResponse(

        @JsonProperty("section_key")
        String sectionKey,

        String title,

        String content
) {
}
