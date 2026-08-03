package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.report.entity.ReportNarrativeEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportNarrativeRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.dto.ReportNarrativeResponse;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 리포트 섹션 서술 저장.
 *
 * <p>서술은 챗봇 근거로만 쓰이는 부수 정보다. 저장에 실패해도 리포트 파일 자체는 이미
 * 만들어졌으므로, 리포트 생성 요청 전체를 실패시키지 않고 로그만 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportNarrativePersistenceService {

    private final AuditRepository auditRepository;
    private final ReportNarrativeRepository narrativeRepository;

    @Transactional
    public void replaceAll(
            Long auditId,
            ReportType reportType,
            List<ReportNarrativeResponse> narratives
    ) {
        if (narratives == null || narratives.isEmpty()) {
            return;
        }

        AuditEntity audit = auditRepository.findById(auditId)
                .orElseThrow(AuditNotFoundException::new);

        // 재생성이면 이전 서술을 남기지 않는다. 남겨두면 챗봇이 지금 리포트에 없는
        // 문장을 근거로 인용하게 된다.
        narrativeRepository.deleteByAudit_IdAndReportType(auditId, reportType);

        List<ReportNarrativeEntity> entities = new ArrayList<>();
        int displayOrder = 0;

        for (ReportNarrativeResponse narrative : narratives) {
            if (isBlank(narrative)) {
                continue;
            }

            entities.add(ReportNarrativeEntity.create(
                    audit,
                    reportType,
                    narrative.sectionKey(),
                    narrative.title(),
                    narrative.content(),
                    displayOrder++
            ));
        }

        narrativeRepository.saveAll(entities);
    }

    /**
     * 리포트 생성 흐름에서 부르는 진입점.
     *
     * <p>서술 저장이 실패해도 리포트 다운로드는 되어야 하므로 예외를 삼킨다. 다만 챗봇이
     * 리포트를 근거로 쓰지 못하게 되므로 로그는 남긴다.
     */
    public void saveQuietly(
            Long auditId,
            ReportType reportType,
            List<ReportNarrativeResponse> narratives
    ) {
        try {
            replaceAll(auditId, reportType, narratives);
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

    private boolean isBlank(ReportNarrativeResponse narrative) {
        return narrative == null
                || narrative.sectionKey() == null || narrative.sectionKey().isBlank()
                || narrative.title() == null || narrative.title().isBlank()
                || narrative.content() == null || narrative.content().isBlank();
    }
}
