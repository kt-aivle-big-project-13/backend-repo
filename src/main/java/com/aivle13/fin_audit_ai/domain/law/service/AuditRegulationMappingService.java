package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditRegulationMappingEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.SelfCheckItemCode;
import com.aivle13.fin_audit_ai.domain.law.dto.AuditRegulationComplianceView;
import com.aivle13.fin_audit_ai.domain.law.dto.MatchedChecklistItem;
import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.AuditRegulationMappingRepository;
import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleRepository;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 자율점검 응답을 정적 매핑표에 대입해 관련 법령 조항을 찾아 audit_law_mappings에 저장/조회한다.
 * compliance는 사람이 검토해서 정하는 게 아니라 해당 항목의 답변으로 즉시 결정된다:
 * 예(true) → COMPLIANT, 아니요(false) → NON_COMPLIANT. 재제출 시 이전 매핑을 전부 지우고
 * 다시 채운다.
 * 자율점검 항목은 {@link SelfCheckItemCode} 5개로 고정돼 있고 매핑 대상 법령도 2개(AI 기본법
 * +시행령)로 닫혀 있어, 매핑 결과 자체가 상수다. 예전엔 이걸 임베딩 유사도 검색(RAG)으로 매번
 * 다시 계산했었는데, 결과가 안 바뀌는 걸 매번 재계산하는 낭비였고 사람이 검수하지 않은 유사도
 * 결과를 법적 근거로 쓰는 것도 리스크였다 — 그래서 사람이 검수한 이 표로 대체했다.
 * 자율점검 문항은 법 문언을 그대로 옮긴 게 아니라 실무 체크리스트로 재구성한 것이라, 조 하나가
 * 여러 문항에 걸리는 경우가 있다(예: 제34조는 위험관리·관리감독·문서화 세 문항에 서로 다른
 * 호로 걸림). 원문에 직접 대응되는 항을 찾지 못한 조항은 억지로 끼워맞추지 않고 매핑에서
 * 제외한다.
 */
@Service
@RequiredArgsConstructor
public class AuditRegulationMappingService implements AuditRegulationMappingQueryService {

    private record ArticleRef(String lawName, String articleNo) {
    }

    // answer가 null이면 자율점검 답변(예/아니요)과 무관하게 항상 근거로 쓰이고, true/false면
    // 그 답변일 때만 근거로 쓰인다(예: 위반 시 과태료 조항은 "아니요"일 때만 의미가 있다).
    // note에는 law_articles 원문을 대조한 근거를 남겨, 법적 효력이 있는 조항(특히 벌칙·과태료)을
    // 팀이 나중에 리뷰할 때 근거를 추적할 수 있게 한다.
    private record LawMapping(ArticleRef article, String clauseNo, Boolean answer, String note) {
    }

    private static final Map<SelfCheckItemCode, List<LawMapping>> ARTICLE_MAPPING = Map.of(
            SelfCheckItemCode.NOTICE, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제31조"), "①", null,
                            "\"제품 또는 서비스가 해당 인공지능에 기반하여 운용된다는 사실을 이용자에게 "
                                    + "사전에 고지하여야 한다\" — 의무 자체라 답변과 무관하게 항상 근거"
                    ),
                    new LawMapping(
                            new ArticleRef("AI 기본법 시행령", "제23조"), "①", null,
                            "제31조①의 구체적 고지 방법(제품 기재·화면표시·게시 등 4가지) 규정 — 답변 무관"
                    ),
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제43조"), "①1호", false,
                            "\"제31조제1항을 위반하여 고지를 이행하지 아니한 자\"에게 과태료 — "
                                    + "위반(아니요) 시에만 근거로서 의미 있음"
                    )
            ),
            SelfCheckItemCode.OBJECTION, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법 시행령", "제25조"), "④", null,
                            "\"회신 결과에 이의가 있을 때에는 회신을 받은 날부터 10일 이내에... "
                                    + "재확인 요청서를 제출해야 한다\" — 절차 존재 자체가 의무라 답변 무관"
                    )
            ),
            SelfCheckItemCode.OVERSIGHT, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①4호", null,
                            "\"고영향 인공지능에 대한 사람의 관리·감독\" — 원문 그대로, 답변 무관"
                    )
            ),
            SelfCheckItemCode.RISK_MANAGEMENT, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제32조"), "①", null,
                            "위험 식별·평가·완화(1호) + 위험관리체계 구축(2호) — 조 전체가 위험관리 규정"
                    ),
                    new LawMapping(
                            new ArticleRef("AI 기본법 시행령", "제27조"), "①1호", null,
                            "\"위험관리정책 및 조직체계 등... 위험관리방안의 주요 내용\""
                    ),
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①1호", null,
                            "\"위험관리방안의 수립·운영\" — 제34조가 문항마다 다른 호로 걸리는 조항 중 하나"
                    )
            ),
            SelfCheckItemCode.DOCUMENTATION, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①5호", null,
                            "\"안전성·신뢰성 확보를 위한 조치의 내용을 확인할 수 있는 문서의 작성과 보관\""
                    ),
                    new LawMapping(
                            new ArticleRef("AI 기본법 시행령", "제27조"), "②", null,
                            "\"그 근거를 문서로 5년간 보관(전자적 방법을 통한 보관을 포함한다)해야 한다\""
                    )
            )
    );

    private final AuditRepository auditRepository;
    private final SelfCheckAnswerRepository selfCheckAnswerRepository;
    private final AuditRegulationMappingRepository auditRegulationMappingRepository;
    private final LawArticleRepository lawArticleRepository;

    @Transactional
    public void mapFromSelfCheckAnswers(Long auditId) {
        // 동일 auditId에 대한 매핑 재생성이 겹치면 delete+insert가 경합해
        // uk_audit_law_mappings_audit_article 유니크 제약을 위반할 수 있다. 감사 행에
        // 비관적 쓰기 잠금을 걸어 트랜잭션이 끝날 때까지 뒤이은 요청을 직렬화한다.
        AuditEntity audit = auditRepository.findByIdForUpdate(auditId)
                .orElseThrow(AuditNotFoundException::new);

        List<SelfCheckAnswerEntity> answers = selfCheckAnswerRepository.findAllByAudit_Id(auditId);

        auditRegulationMappingRepository.deleteAllByAudit_Id(auditId);

        Map<Long, AuditRegulationMappingEntity> mappings = new LinkedHashMap<>();

        for (SelfCheckAnswerEntity answer : answers) {
            ComplianceStatus compliance = answer.isAnswer()
                    ? ComplianceStatus.COMPLIANT
                    : ComplianceStatus.NON_COMPLIANT;

            for (LawMapping lawMapping : applicableMappings(answer)) {
                LawArticleEntity article = lawArticleRepository
                        .findByLawNameAndArticleNo(lawMapping.article().lawName(), lawMapping.article().articleNo())
                        .orElseThrow(() -> new IllegalStateException(
                                "정적 매핑표에 정의된 조항을 law_articles에서 찾을 수 없습니다: "
                                        + lawMapping.article().lawName() + " " + lawMapping.article().articleNo()));

                if (mappings.containsKey(article.getId())) {
                    continue;
                }

                mappings.put(article.getId(), AuditRegulationMappingEntity.of(
                        audit,
                        article,
                        compliance,
                        "자율점검 '" + answer.getItemCode().label() + "' 항목(답변: "
                                + (answer.isAnswer() ? "예" : "아니요") + ") 기반 자동 매칭"
                ));
            }
        }

        auditRegulationMappingRepository.saveAll(mappings.values());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditRegulationComplianceView> getMappings(Long auditId) {
        List<AuditRegulationMappingEntity> mappings = auditRegulationMappingRepository.findAllByAudit_Id(auditId);
        Map<ArticleRef, List<MatchedChecklistItem>> matchedItemsByArticle = resolveMatchedItemsByArticle(auditId);

        return mappings.stream()
                .map(mapping -> {
                    ArticleRef ref = new ArticleRef(
                            mapping.getArticle().getLawName(), mapping.getArticle().getArticleNo());

                    return AuditRegulationComplianceView.from(
                            mapping, matchedItemsByArticle.getOrDefault(ref, List.of()));
                })
                .toList();
    }

    // audit_law_mappings에는 어느 문항·항에서 나온 매칭인지 저장하지 않는다(조문 하나가 여러
    // 문항에, 문항마다 다른 항으로 걸릴 수 있어 — 예: 제34조 — (audit_id, article_id) 유니크
    // 제약을 둔 이 테이블 구조로는 전부 담을 수 없다). 대신 자율점검 답변을 ARTICLE_MAPPING에
    // 그대로 다시 대입해 조회할 때마다 역산한다.
    private Map<ArticleRef, List<MatchedChecklistItem>> resolveMatchedItemsByArticle(Long auditId) {
        List<SelfCheckAnswerEntity> answers = selfCheckAnswerRepository.findAllByAudit_Id(auditId);
        Map<ArticleRef, List<MatchedChecklistItem>> matchedItemsByArticle = new LinkedHashMap<>();

        for (SelfCheckAnswerEntity answer : answers) {
            for (LawMapping lawMapping : applicableMappings(answer)) {
                matchedItemsByArticle
                        .computeIfAbsent(lawMapping.article(), key -> new ArrayList<>())
                        .add(new MatchedChecklistItem(answer.getItemCode(), lawMapping.clauseNo()));
            }
        }

        return matchedItemsByArticle;
    }

    // answer가 null인 매핑은 자율점검 답변과 무관하게 항상 적용되고, true/false인 매핑은 그
    // 답변일 때만 적용된다.
    private List<LawMapping> applicableMappings(SelfCheckAnswerEntity answer) {
        return ARTICLE_MAPPING.getOrDefault(answer.getItemCode(), List.of()).stream()
                .filter(lawMapping -> lawMapping.answer() == null || lawMapping.answer() == answer.isAnswer())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditRegulationComplianceView> getMappings(Long userId, Long auditId) {
        auditRepository.findByIdAndUser_Id(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);

        return getMappings(auditId);
    }
}
