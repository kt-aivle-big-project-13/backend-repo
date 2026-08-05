package com.aivle13.fin_audit_ai.domain.chat.service;

import com.aivle13.fin_audit_ai.domain.report.entity.ReportNarrativeEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportNarrativeRepository;
import com.aivle13.fin_audit_ai.global.ai.dto.chat.request.ChatAnswerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 저장된 리포트 섹션 서술을 질의 응답 근거로 넘긴다.
 *
 * <p>리포트 파일은 S3 에 있고 본문 텍스트는 리포트 생성 시 따로 저장해 둔다. 여기서는
 * 그 저장분을 읽어 옮기기만 하며, 파일을 내려받거나 파싱하지 않는다.
 *
 * <p>별도 빈으로 둔 이유는 {@link ChatFactAssembler} 와 같다 — 같은 클래스 자기호출은
 * 프록시를 거치지 않아 {@code @Transactional} 이 적용되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ChatReportSectionLoader {

    private final ReportNarrativeRepository narrativeRepository;

    @Transactional(readOnly = true)
    public List<ChatAnswerRequest.ReportSection> load(Long auditId) {
        return narrativeRepository
                .findAllByAudit_IdOrderByReportTypeAscDisplayOrderAsc(auditId)
                .stream()
                .map(ChatReportSectionLoader::toReportSection)
                .toList();
    }

    private static ChatAnswerRequest.ReportSection toReportSection(
            ReportNarrativeEntity narrative
    ) {
        return new ChatAnswerRequest.ReportSection(
                narrative.getReportType().name(),
                narrative.getSectionKey(),
                narrative.getTitle(),
                narrative.getContent()
        );
    }
}
