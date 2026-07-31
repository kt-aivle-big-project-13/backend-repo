package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditRegulationMappingEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.SelfCheckItemCode;
import com.aivle13.fin_audit_ai.domain.law.dto.AuditRegulationComplianceView;
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
 */
@Service
@RequiredArgsConstructor
public class AuditRegulationMappingService implements AuditRegulationMappingQueryService {

    private record ArticleRef(String lawName, String articleNo) {
    }

    private static Map<Boolean, List<ArticleRef>> sameForBothAnswers(List<ArticleRef> refs) {
        return Map.of(true, refs, false, refs);
    }

    private static final Map<SelfCheckItemCode, Map<Boolean, List<ArticleRef>>> ARTICLE_MAPPING = Map.of(
            SelfCheckItemCode.NOTICE, Map.of(
                    true, List.of(
                            new ArticleRef("AI 기본법", "제6조"),
                            new ArticleRef("AI 기본법", "제12조"),
                            new ArticleRef("AI 기본법", "제31조"),
                            new ArticleRef("AI 기본법", "제40조"),
                            new ArticleRef("AI 기본법 시행령", "제23조")
                    ),
                    false, List.of(
                            new ArticleRef("AI 기본법", "제6조"),
                            new ArticleRef("AI 기본법", "제12조"),
                            new ArticleRef("AI 기본법", "제31조"),
                            new ArticleRef("AI 기본법", "제40조"),
                            new ArticleRef("AI 기본법", "제43조"),
                            new ArticleRef("AI 기본법 시행령", "제23조")
                    )
            ),
            SelfCheckItemCode.OBJECTION, sameForBothAnswers(List.of(
                    new ArticleRef("AI 기본법", "제20조"),
                    new ArticleRef("AI 기본법 시행령", "제3조"),
                    new ArticleRef("AI 기본법 시행령", "제25조")
            )),
            SelfCheckItemCode.OVERSIGHT, sameForBothAnswers(List.of(
                    new ArticleRef("AI 기본법", "제23조"),
                    new ArticleRef("AI 기본법", "제27조"),
                    new ArticleRef("AI 기본법", "제34조"),
                    new ArticleRef("AI 기본법 시행령", "제18조")
            )),
            SelfCheckItemCode.RISK_MANAGEMENT, sameForBothAnswers(List.of(
                    new ArticleRef("AI 기본법", "제32조"),
                    new ArticleRef("AI 기본법 시행령", "제10조"),
                    new ArticleRef("AI 기본법 시행령", "제27조")
            )),
            SelfCheckItemCode.DOCUMENTATION, sameForBothAnswers(List.of(
                    new ArticleRef("AI 기본법 시행령", "제6조"),
                    new ArticleRef("AI 기본법 시행령", "제13조")
            ))
    );

    // 조 전체가 아니라 특정 항만 실제로 해당 문항과 관련 있는 경우의 표시용 라벨.
    // ArticleRef의 equals/hashCode(법령명+조번호)에는 영향을 주지 않도록 별도 맵으로 분리했다
    // — ArticleRef에 항 필드를 직접 추가하면 resolveItemCodesByArticle()에서 LawArticleEntity로
    // 새로 만드는 조회용 키(항 정보 없음)와 안 맞아 조회가 깨진다.
    // 매칭 로직과 무관한 순수 표시용이라, 조 단위 조문 검토 후 해당 항만 사람이 채워 넣는다.
    private static final Map<ArticleRef, String> PARAGRAPH_LABELS = Map.of();

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
            List<ArticleRef> refs = ARTICLE_MAPPING
                    .getOrDefault(answer.getItemCode(), Map.of())
                    .getOrDefault(answer.isAnswer(), List.of());

            ComplianceStatus compliance = answer.isAnswer()
                    ? ComplianceStatus.COMPLIANT
                    : ComplianceStatus.NON_COMPLIANT;

            for (ArticleRef ref : refs) {
                LawArticleEntity article = lawArticleRepository
                        .findByLawNameAndArticleNo(ref.lawName(), ref.articleNo())
                        .orElseThrow(() -> new IllegalStateException(
                                "정적 매핑표에 정의된 조항을 law_articles에서 찾을 수 없습니다: "
                                        + ref.lawName() + " " + ref.articleNo()));

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
        Map<ArticleRef, List<SelfCheckItemCode>> itemCodesByArticle = resolveItemCodesByArticle(auditId);

        return mappings.stream()
                .map(mapping -> {
                    ArticleRef ref = new ArticleRef(
                            mapping.getArticle().getLawName(), mapping.getArticle().getArticleNo());

                    return AuditRegulationComplianceView.from(
                            mapping,
                            itemCodesByArticle.getOrDefault(ref, List.of()),
                            PARAGRAPH_LABELS.get(ref));
                })
                .toList();
    }

    // audit_law_mappings에는 어느 문항에서 나온 매칭인지 저장하지 않는다(조문 하나가 여러 문항에
    // 걸치면 audit_law_mappings의 (audit_id, article_id) 유니크 제약상 한쪽 문항 정보가 사라지기
    // 때문). 대신 자율점검 답변을 ARTICLE_MAPPING에 그대로 다시 대입해 조회할 때마다 역산한다.
    private Map<ArticleRef, List<SelfCheckItemCode>> resolveItemCodesByArticle(Long auditId) {
        List<SelfCheckAnswerEntity> answers = selfCheckAnswerRepository.findAllByAudit_Id(auditId);
        Map<ArticleRef, List<SelfCheckItemCode>> itemCodesByArticle = new LinkedHashMap<>();

        for (SelfCheckAnswerEntity answer : answers) {
            List<ArticleRef> refs = ARTICLE_MAPPING
                    .getOrDefault(answer.getItemCode(), Map.of())
                    .getOrDefault(answer.isAnswer(), List.of());

            for (ArticleRef ref : refs) {
                itemCodesByArticle.computeIfAbsent(ref, key -> new ArrayList<>()).add(answer.getItemCode());
            }
        }

        return itemCodesByArticle;
    }

    @Transactional(readOnly = true)
    public List<AuditRegulationComplianceView> getMappings(Long userId, Long auditId) {
        auditRepository.findByIdAndUser_Id(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);

        return getMappings(auditId);
    }
}
