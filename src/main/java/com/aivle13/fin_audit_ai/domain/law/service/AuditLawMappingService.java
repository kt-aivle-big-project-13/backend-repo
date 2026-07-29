package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditLawMappingEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.law.dto.AuditLawComplianceView;
import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.AuditLawMappingRepository;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 자율점검 응답을 질의로 삼아 관련 법령 조항을 찾아 audit_law_mappings에 저장/조회한다.
 * compliance는 사람이 검토해서 정하는 게 아니라 해당 항목의 답변으로 즉시 결정된다:
 * 예(true) → COMPLIANT, 아니요(false) → NON_COMPLIANT. 재제출 시 이전 매핑을 전부 지우고
 * 다시 채운다.
 */
@Service
@RequiredArgsConstructor
public class AuditLawMappingService implements AuditLawMappingQueryService {

    private static final int TOP_K = 5;

    private final AuditRepository auditRepository;
    private final SelfCheckAnswerRepository selfCheckAnswerRepository;
    private final AuditLawMappingRepository auditLawMappingRepository;
    private final LawArticleSearchService lawArticleSearchService;

    @Transactional
    public void mapFromSelfCheckAnswers(Long auditId) {
        // 동일 auditId에 대한 매핑 재생성이 겹치면 delete+insert가 경합해
        // uk_audit_law_mappings_audit_article 유니크 제약을 위반할 수 있다. 감사 행에
        // 비관적 쓰기 잠금을 걸어 트랜잭션이 끝날 때까지 뒤이은 요청을 직렬화한다.
        AuditEntity audit = auditRepository.findByIdForUpdate(auditId)
                .orElseThrow(AuditNotFoundException::new);

        List<SelfCheckAnswerEntity> answers = selfCheckAnswerRepository.findAllByAudit_Id(auditId);

        auditLawMappingRepository.deleteAllByAudit_Id(auditId);

        Map<Long, AuditLawMappingEntity> mappings = new LinkedHashMap<>();

        for (SelfCheckAnswerEntity answer : answers) {
            String queryText = answer.getItemCode().label();

            List<LawArticleEntity> similarArticles =
                    lawArticleSearchService.searchSimilarArticles(queryText, TOP_K);

            ComplianceStatus compliance = answer.isAnswer()
                    ? ComplianceStatus.COMPLIANT
                    : ComplianceStatus.NON_COMPLIANT;

            for (LawArticleEntity article : similarArticles) {
                if (mappings.containsKey(article.getId())) {
                    continue;
                }

                mappings.put(article.getId(), AuditLawMappingEntity.of(
                        audit,
                        article,
                        compliance,
                        "자율점검 '" + answer.getItemCode().label() + "' 항목(답변: "
                                + (answer.isAnswer() ? "예" : "아니요") + ") 기반 자동 매칭"
                ));
            }
        }

        auditLawMappingRepository.saveAll(mappings.values());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLawComplianceView> getMappings(Long auditId) {
        return auditLawMappingRepository.findAllByAudit_Id(auditId).stream()
                .map(AuditLawComplianceView::from)
                .toList();
    }
}
