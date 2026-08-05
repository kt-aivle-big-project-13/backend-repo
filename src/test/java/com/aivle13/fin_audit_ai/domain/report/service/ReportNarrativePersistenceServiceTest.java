package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.report.entity.ReportNarrativeEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportNarrativeRepository;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportNarrativePersistenceService;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.ReportNarrativeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportNarrativePersistenceServiceTest {

    private static final Long AUDIT_ID = 42L;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private ReportNarrativeRepository narrativeRepository;

    @Mock
    private AuditEntity audit;

    @InjectMocks
    private ReportNarrativePersistenceService service;

    @Test
    @DisplayName("받은 순서를 표시 순서로 저장한다")
    void savesNarrativesInReceivedOrder() {
        given(auditRepository.findById(AUDIT_ID)).willReturn(Optional.of(audit));

        service.replaceAll(AUDIT_ID, ReportType.BIAS_REPORT, List.of(
                new ReportNarrativeResponse("overview_purpose", "1. 감사 개요", "개요 서술"),
                new ReportNarrativeResponse("metric_results", "5. 공정성 지표 결과", "지표 서술")
        ));

        List<ReportNarrativeEntity> saved = captureSaved();

        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getSectionKey()).isEqualTo("overview_purpose");
        assertThat(saved.get(0).getDisplayOrder()).isZero();
        assertThat(saved.get(1).getSectionKey()).isEqualTo("metric_results");
        assertThat(saved.get(1).getDisplayOrder()).isEqualTo(1);
        assertThat(saved.get(1).getTitle()).isEqualTo("5. 공정성 지표 결과");
    }

    @Test
    @DisplayName("재생성 시 같은 리포트의 이전 서술을 먼저 지운다")
    void replacesPreviousNarrativesOfSameReport() {
        given(auditRepository.findById(AUDIT_ID)).willReturn(Optional.of(audit));

        service.replaceAll(AUDIT_ID, ReportType.BIAS_REPORT, List.of(
                new ReportNarrativeResponse("overview_purpose", "1. 감사 개요", "개요 서술")
        ));

        verify(narrativeRepository)
                .deleteByAudit_IdAndReportType(AUDIT_ID, ReportType.BIAS_REPORT);
    }

    @Test
    @DisplayName("서술이 비어 있으면 기존 서술을 지우지 않는다")
    void keepsPreviousNarrativesWhenNothingReceived() {
        service.replaceAll(AUDIT_ID, ReportType.BIAS_REPORT, List.of());
        service.replaceAll(AUDIT_ID, ReportType.BIAS_REPORT, null);

        verify(narrativeRepository, never())
                .deleteByAudit_IdAndReportType(anyLong(), any());
        verify(narrativeRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("내용이 빠진 서술은 저장하지 않는다")
    void skipsIncompleteNarratives() {
        given(auditRepository.findById(AUDIT_ID)).willReturn(Optional.of(audit));

        service.replaceAll(AUDIT_ID, ReportType.XAI_REPORT, List.of(
                new ReportNarrativeResponse("overview_purpose", "1. 감사 개요", "개요 서술"),
                new ReportNarrativeResponse("results_summary", "2. 결과 요약", "   "),
                new ReportNarrativeResponse(null, "3. 제목만", "내용")
        ));

        List<ReportNarrativeEntity> saved = captureSaved();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getSectionKey()).isEqualTo("overview_purpose");
    }

    @Test
    @DisplayName("받은 서술이 전부 쓸 수 없으면 기존 서술을 지우지 않는다")
    void keepsPreviousNarrativesWhenAllReceivedAreUnusable() {
        service.replaceAll(AUDIT_ID, ReportType.BIAS_REPORT, List.of(
                new ReportNarrativeResponse("overview_purpose", "1. 감사 개요", "   "),
                new ReportNarrativeResponse(null, "5. 공정성 지표 결과", "지표 서술"),
                new ReportNarrativeResponse("tradeoff", "", "트레이드오프 서술")
        ));

        // 지우기만 하면 대체할 내용도 없이 챗봇 근거만 사라진다.
        verify(narrativeRepository, never())
                .deleteByAudit_IdAndReportType(anyLong(), any());
        verify(narrativeRepository, never()).saveAll(any());
    }

    @SuppressWarnings("unchecked")
    private List<ReportNarrativeEntity> captureSaved() {
        ArgumentCaptor<List<ReportNarrativeEntity>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(narrativeRepository).saveAll(captor.capture());

        return captor.getValue();
    }
}
