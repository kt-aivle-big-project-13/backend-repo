package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.dto.ReportNarrativeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportNarrativeRecorderTest {

    private static final Long AUDIT_ID = 42L;

    private static final List<ReportNarrativeResponse> NARRATIVES = List.of(
            new ReportNarrativeResponse("overview_purpose", "1. 감사 개요", "개요 서술")
    );

    @Mock
    private ReportNarrativePersistenceService persistenceService;

    @InjectMocks
    private ReportNarrativeRecorder recorder;

    @Test
    @DisplayName("받은 서술을 저장 서비스에 그대로 넘긴다")
    void delegatesToPersistenceService() {
        recorder.record(AUDIT_ID, ReportType.BIAS_REPORT, NARRATIVES);

        verify(persistenceService)
                .replaceAll(AUDIT_ID, ReportType.BIAS_REPORT, NARRATIVES);
    }

    @Test
    @DisplayName("서술 저장이 실패해도 리포트 생성 흐름을 막지 않는다")
    void swallowsPersistenceFailure() {
        willThrow(new RuntimeException("DB 장애"))
                .given(persistenceService)
                .replaceAll(AUDIT_ID, ReportType.BIAS_REPORT, NARRATIVES);

        assertThatCode(() ->
                recorder.record(AUDIT_ID, ReportType.BIAS_REPORT, NARRATIVES)
        ).doesNotThrowAnyException();
    }
}
