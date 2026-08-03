package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.dto.ReportNarrativeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 리포트 생성 흐름에서 서술 저장을 부르는 진입점.
 *
 * <p>서술은 챗봇 근거로만 쓰이는 부수 정보다. 저장에 실패해도 리포트 파일은 이미 만들어졌으므로
 * 리포트 생성 요청 전체를 실패시키지 않고 로그만 남긴다.
 *
 * <p>저장 서비스와 별도 빈으로 둔 이유는 같은 클래스 자기호출이 프록시를 거치지 않아
 * {@code @Transactional} 이 적용되지 않기 때문이다. 한 클래스에 두면 삭제와 저장이 서로 다른
 * 트랜잭션으로 나뉘어, 저장이 실패했을 때 기존 서술만 사라진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportNarrativeRecorder {

    private final ReportNarrativePersistenceService persistenceService;

    public void record(
            Long auditId,
            ReportType reportType,
            List<ReportNarrativeResponse> narratives
    ) {
        try {
            persistenceService.replaceAll(auditId, reportType, narratives);
        } catch (RuntimeException exception) {
            log.warn(
                    "리포트 섹션 서술 저장에 실패했습니다. 챗봇이 이 리포트를 근거로 쓰지 못합니다. "
                            + "auditId={}, reportType={}",
                    auditId,
                    reportType,
                    exception
            );
        }
    }
}
