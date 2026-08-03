package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.report.entity.ReportNarrativeEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportNarrativeRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.dto.ReportNarrativeResponse;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 리포트 섹션 서술 저장.
 *
 * <p>삭제와 저장이 한 트랜잭션에 묶여야 한다. 나뉘면 삭제만 커밋된 뒤 저장이 실패했을 때
 * 기존 서술이 대체되지 않고 사라진다. 실패를 삼키는 처리는 {@link ReportNarrativeRecorder}
 * 가 맡는다 — 같은 클래스에 두면 자기호출이라 프록시를 거치지 않아 트랜잭션이 걸리지 않는다.
 */
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
        List<ReportNarrativeResponse> usable = usableOnly(narratives);

        // 쓸 수 있는 서술이 하나도 없으면 기존 서술을 건드리지 않는다. 지우기만 하면
        // 대체할 내용도 없이 챗봇 근거만 사라진다.
        if (usable.isEmpty()) {
            return;
        }

        AuditEntity audit = auditRepository.findById(auditId)
                .orElseThrow(AuditNotFoundException::new);

        // 재생성이면 이전 서술을 남기지 않는다. 남겨두면 챗봇이 지금 리포트에 없는
        // 문장을 근거로 인용하게 된다.
        narrativeRepository.deleteByAudit_IdAndReportType(auditId, reportType);

        narrativeRepository.saveAll(toEntities(audit, reportType, usable));
    }

    private List<ReportNarrativeResponse> usableOnly(
            List<ReportNarrativeResponse> narratives
    ) {
        if (narratives == null) {
            return List.of();
        }

        return narratives.stream()
                .filter(ReportNarrativePersistenceService::isUsable)
                .toList();
    }

    private List<ReportNarrativeEntity> toEntities(
            AuditEntity audit,
            ReportType reportType,
            List<ReportNarrativeResponse> narratives
    ) {
        List<ReportNarrativeEntity> entities = new ArrayList<>();
        int displayOrder = 0;

        for (ReportNarrativeResponse narrative : narratives) {
            entities.add(ReportNarrativeEntity.create(
                    audit,
                    reportType,
                    narrative.sectionKey(),
                    narrative.title(),
                    narrative.content(),
                    displayOrder++
            ));
        }

        return entities;
    }

    private static boolean isUsable(ReportNarrativeResponse narrative) {
        return narrative != null
                && hasText(narrative.sectionKey())
                && hasText(narrative.title())
                && hasText(narrative.content());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
