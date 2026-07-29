package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditLawMappingEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.AuditLawMappingRepository;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 자율점검 응답을 질의로 삼아 관련 법령 조항 후보를 찾아 audit_law_mappings에 PENDING으로 저장한다.
 * "후보 제시"일 뿐 최종 판정이 아니므로, 사람이 이미 검토를 마친(PENDING이 아닌) 매핑은 건드리지
 * 않고 PENDING 매핑만 지우고 다시 채운다.
 */
@Service
@RequiredArgsConstructor
public class AuditLawMappingService {

    private static final int TOP_K = 5;

    private final AuditRepository auditRepository;
    private final SelfCheckAnswerRepository selfCheckAnswerRepository;
    private final AuditLawMappingRepository auditLawMappingRepository;
    private final LawArticleSearchService lawArticleSearchService;

    @Transactional
    public void mapFromSelfCheckAnswers(Long auditId) {
        // 동일 auditId에 대한 매핑 재생성이 겹치면 PENDING delete+insert가 경합해
        // uk_audit_law_mappings_audit_article 유니크 제약을 위반할 수 있다. 감사 행에
        // 비관적 쓰기 잠금을 걸어 트랜잭션이 끝날 때까지 뒤이은 요청을 직렬화한다.
        AuditEntity audit = auditRepository.findByIdForUpdate(auditId)
                .orElseThrow(AuditNotFoundException::new);

        List<SelfCheckAnswerEntity> answers = selfCheckAnswerRepository.findAllByAudit_Id(auditId);

        Set<Long> decidedArticleIds = auditLawMappingRepository.findAllByAudit_Id(auditId).stream()
                .filter(mapping -> mapping.getCompliance() != ComplianceStatus.PENDING)
                .map(mapping -> mapping.getArticle().getId())
                .collect(Collectors.toSet());

        auditLawMappingRepository.deleteAllByAudit_IdAndCompliance(auditId, ComplianceStatus.PENDING);

        Map<Long, AuditLawMappingEntity> candidates = new LinkedHashMap<>();

        for (SelfCheckAnswerEntity answer : answers) {
            String queryText = answer.getItemCode().label();

            List<LawArticleEntity> similarArticles =
                    lawArticleSearchService.searchSimilarArticles(queryText, TOP_K);

            for (LawArticleEntity article : similarArticles) {
                if (decidedArticleIds.contains(article.getId())
                        || candidates.containsKey(article.getId())) {
                    continue;
                }

                candidates.put(article.getId(), AuditLawMappingEntity.of(
                        audit,
                        article,
                        ComplianceStatus.PENDING,
                        "자율점검 '" + answer.getItemCode().label() + "' 항목(답변: "
                                + (answer.isAnswer() ? "예" : "아니요") + ") 기반 자동 매칭 후보"
                ));
            }
        }

        auditLawMappingRepository.saveAll(candidates.values());
    }
}
