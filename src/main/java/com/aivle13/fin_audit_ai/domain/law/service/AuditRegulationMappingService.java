package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditRegulationMappingEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.SelfCheckAnswerValue;
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
 * 예 → COMPLIANT, 아니요 → NON_COMPLIANT, 해당없음 → PENDING(판정 보류 — 이 법 조항 자체가
 * 이 회사에 적용되는지 여부라 준수/미준수를 가릴 대상이 아님). 재제출 시 이전 매핑을 전부
 * 지우고 다시 채운다.
 * 자율점검 항목은 {@link SelfCheckItemCode} 21개로 고정돼 있고 매핑 대상 법령도 2개(AI 기본법
 * +시행령)로 닫혀 있어, 매핑 결과 자체가 상수다. 예전엔 이걸 임베딩 유사도 검색(RAG)으로 매번
 * 다시 계산했었는데, 결과가 안 바뀌는 걸 매번 재계산하는 낭비였고 사람이 검수하지 않은 유사도
 * 결과를 법적 근거로 쓰는 것도 리스크였다 — 그래서 사람이 검수한 이 표로 대체했다.
 * 자율점검 문항은 법 문언을 그대로 옮긴 게 아니라 실무 체크리스트로 재구성한 것이라, 조 하나가
 * 여러 문항에 걸리는 경우가 있다(예: 제34조는 위험관리·관리감독·문서화 등 여러 문항에 서로
 * 다른 호로 걸림). 원문에 직접 대응되는 항을 찾지 못한 조항은 억지로 끼워맞추지 않고 매핑에서
 * 제외한다.
 */
@Service
@RequiredArgsConstructor
public class AuditRegulationMappingService implements AuditRegulationMappingQueryService {

    private record ArticleRef(String lawName, String articleNo) {
    }

    // answer가 null이면 자율점검 답변(예/아니요)과 무관하게 항상 근거로 쓰이고, true/false면
    // 그 답변일 때만 근거로 쓰인다(예: 위반 시 과태료 조항은 "아니요"일 때만 의미가 있다).
    // note는 MatchedChecklistItem을 통해 API 응답으로 그대로 노출되는 화면 표시용 요약문이다
    // — 조 원문을 팀이 검토해 완성된 문장으로 다듬은 것이며, LLM 요약이나 임의 재해석이 아니다.
    private record LawMapping(ArticleRef article, String clauseNo, Boolean answer, String note) {
    }

    private static final Map<SelfCheckItemCode, List<LawMapping>> ARTICLE_MAPPING = Map.ofEntries(
            // TR · 투명성 확보
            Map.entry(SelfCheckItemCode.TR_01, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제31조"), "①", null,
                            "인공지능사업자는 고영향 인공지능이나 생성형 인공지능을 이용한 제품·서비스를 "
                                    + "제공하려는 경우, 해당 인공지능에 기반하여 운용된다는 사실을 이용자에게 "
                                    + "사전에 고지해야 합니다."
                    ),
                    new LawMapping(
                            new ArticleRef("AI 기본법 시행령", "제23조"), "①", null,
                            "제품에 직접 기재하거나 화면 표시, 게시 등의 방법으로 사전에 고지해야 합니다."
                    ),
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제43조"), "①1호", false,
                            "제31조① 고지 의무를 위반하면 3천만원 이하의 과태료가 부과됩니다."
                    )
            )),
            Map.entry(SelfCheckItemCode.TR_02, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①2호", null,
                            "인공지능의 개발·활용에 사용된 학습용데이터의 개요 등에 대한 설명 방안을 "
                                    + "수립·시행해야 합니다. (최종결과·주요 기준 설명은 설명가능성 리포트에서 "
                                    + "별도 판정)"
                    )
            )),
            Map.entry(SelfCheckItemCode.TR_03, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①", null,
                            "고영향 인공지능의 안전성·신뢰성을 확보하기 위한 조치를 대통령령으로 정하는 "
                                    + "바에 따라 이행해야 합니다. (위험관리방안·이용자 보호방안·담당자 정보 "
                                    + "게시 항목·방법은 시행령 소관)"
                    )
            )),
            Map.entry(SelfCheckItemCode.TR_04, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제31조"), "②", null,
                            "생성형 인공지능 또는 이를 이용한 제품·서비스를 제공하는 경우 그 결과물이 "
                                    + "생성형 인공지능에 의해 생성되었다는 사실을 표시해야 합니다."
                    )
            )),

            // RM · 위험관리
            Map.entry(SelfCheckItemCode.RM_01, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①1호", null,
                            "위험관리방안을 수립·운영해야 합니다."
                    ),
                    new LawMapping(
                            new ArticleRef("AI 기본법 시행령", "제27조"), "①1호", null,
                            "위험관리정책·조직체계 등 위험관리방안의 주요 내용을 게시해야 합니다."
                    )
            )),
            Map.entry(SelfCheckItemCode.RM_02, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①1호", null,
                            "AI 오류·이상 징후에 대한 상시 모니터링과 사고 발생 시 대응은 위험관리방안의 "
                                    + "운영에 포함됩니다."
                    )
            )),
            Map.entry(SelfCheckItemCode.RM_03, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①1호", null,
                            "위험관리방안의 운영에는 모델 재학습·중대 변경 시의 위험 재평가가 포함됩니다."
                    ),
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제32조"), "①1호", null,
                            "인공지능 수명주기 전반에 걸쳐 위험을 식별·평가·완화해야 합니다. (학습 누적 "
                                    + "연산량이 대통령령 기준 이상인 경우 적용)"
                    )
            )),
            Map.entry(SelfCheckItemCode.RM_04, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①6호", null,
                            "위원회에서 심의·의결된 사항도 안전성·신뢰성 확보 조치에 포함됩니다."
                    ),
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "②", null,
                            "장관이 조치의 구체적인 사항을 고시하고 준수를 권고할 수 있습니다."
                    )
            )),

            // UP · 이용자 보호
            Map.entry(SelfCheckItemCode.UP_01, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법 시행령", "제25조"), "④", null,
                            "회신 결과에 이의가 있을 때에는 회신을 받은 날부터 10일 이내에 재확인 요청서를 "
                                    + "제출해야 합니다."
                    )
            )),
            Map.entry(SelfCheckItemCode.UP_02, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①3호", null,
                            "이용자 보호 방안에는 AI로 인한 피해 발생 시 구제·보상 절차가 포함됩니다."
                    )
            )),

            // HO · 사람의 관리·감독
            Map.entry(SelfCheckItemCode.HO_01, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①4호", null,
                            "고영향 인공지능에 대해 사람이 관리·감독하는 조치를 이행해야 합니다."
                    )
            )),
            Map.entry(SelfCheckItemCode.HO_02, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①4호", null,
                            "사람의 관리·감독에는 AI 심사 결과에 대한 개입·번복 이력의 확인이 포함됩니다."
                    ),
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①5호", null,
                            "안전성·신뢰성 확보 조치 내용을 확인할 수 있는 문서를 작성·보관해야 합니다."
                    )
            )),

            // DC · 문서 작성·보관
            Map.entry(SelfCheckItemCode.DC_01, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①5호", null,
                            "안전성·신뢰성 확보 조치 내용을 확인할 수 있는 문서를 작성·보관해야 합니다."
                    ),
                    new LawMapping(
                            new ArticleRef("AI 기본법 시행령", "제27조"), "②", null,
                            "고영향 인공지능에 대한 안전성·신뢰성 확보 조치를 이행하고 그 근거를 문서로 "
                                    + "5년간 보관(전자적 방법 포함)해야 합니다."
                    )
            )),
            Map.entry(SelfCheckItemCode.DC_02, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "①5호", null,
                            "안전성·신뢰성 확보 조치 내용을 확인할 수 있는 문서를 작성·보관해야 합니다."
                    ),
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제36조"), "①3호", null,
                            "국내대리인의 지원 범위에는 문서의 최신성·정확성에 대한 점검이 포함됩니다."
                    )
            )),

            // IA · 영향평가 (전부 노력의무 — '아니오'여도 위반이 아님)
            Map.entry(SelfCheckItemCode.IA_01, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제35조"), "①", null,
                            "고영향 인공지능을 이용한 서비스를 제공하기 전에 사람의 기본권에 미치는 영향을 "
                                    + "평가하기 위하여 노력하여야 합니다. (노력의무)"
                    )
            )),
            Map.entry(SelfCheckItemCode.IA_02, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제35조"), "①", null,
                            "영향평가에는 인공지능취약계층의 특성이 반영될 수 있도록 해야 합니다. "
                                    + "(2026. 1. 20. 개정, 노력의무)"
                    )
            )),

            // SC · 적용범위·사업자 지위
            Map.entry(SelfCheckItemCode.SC_01, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제2조"), "7호", null,
                            "인공지능사업자는 인공지능개발사업자와 인공지능이용사업자로 구분됩니다."
                    )
            )),
            Map.entry(SelfCheckItemCode.SC_02, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제34조"), "③", null,
                            "다른 법령에 따라 준하는 조치를 이행한 경우 이 법의 조치를 이행한 것으로 "
                                    + "인정됩니다."
                    )
            )),
            Map.entry(SelfCheckItemCode.SC_03, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제36조"), "①", null,
                            "국내에 주소·영업소가 없는 사업자로서 이용자 수·매출액 등이 대통령령 기준에 "
                                    + "해당하면 국내대리인을 지정·신고해야 합니다."
                    )
            )),
            Map.entry(SelfCheckItemCode.SC_04, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제30조"), "③", null,
                            "고영향 인공지능을 제공하는 경우 사전에 검·인증등을 받도록 노력하여야 합니다. "
                                    + "(노력의무)"
                    )
            )),
            Map.entry(SelfCheckItemCode.SC_05, List.of(
                    new LawMapping(
                            new ArticleRef("AI 기본법", "제32조"), "①", null,
                            "학습에 사용된 누적 연산량이 대통령령 기준 이상인 인공지능시스템은 안전성 "
                                    + "확보 조치를 이행해야 합니다."
                    )
            ))
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

        // deleteAllByAudit_Id는 @Modifying이 없는 파생 delete라 영속성 컨텍스트에 삭제만
        // 큐잉되고, Hibernate는 flush 시 삭제보다 삽입을 먼저 내보낸다. flush 없이 바로
        // saveAll을 호출하면 재생성 시(기존 매핑이 이미 있는 상태) INSERT가 아직 지워지지 않은
        // 기존 행과 uk_audit_law_mappings_audit_article 유니크 제약에서 충돌한다 — 그래서
        // delete를 먼저 DB에 반영시킨 뒤 insert한다.
        auditRegulationMappingRepository.deleteAllByAudit_Id(auditId);
        auditRegulationMappingRepository.flush();

        Map<Long, AuditRegulationMappingEntity> mappings = new LinkedHashMap<>();

        for (SelfCheckAnswerEntity answer : answers) {
            ComplianceStatus compliance = toComplianceStatus(answer.getAnswer());

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
                                + answer.getAnswer().label() + ") 기반 자동 매칭"
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
                        .add(new MatchedChecklistItem(answer.getItemCode(), lawMapping.clauseNo(), lawMapping.note()));
            }
        }

        return matchedItemsByArticle;
    }

    // answer가 null인 매핑은 자율점검 답변과 무관하게 항상 적용된다. true/false인 매핑(예:
    // 위반 시 과태료 조항)은 그 답변일 때만 적용되는데, "해당없음"은 예/아니오 어느 쪽도
    // 아니므로 이런 답변-조건부 매핑은 전부 걸러진다 — 해당없음은 "판단 대상이 아니다"는
    // 뜻이라, 그 답변에 따라서만 걸리는 근거(예: 위반 과태료)를 매길 수 없기 때문이다.
    private List<LawMapping> applicableMappings(SelfCheckAnswerEntity answer) {
        return ARTICLE_MAPPING.getOrDefault(answer.getItemCode(), List.of()).stream()
                .filter(lawMapping -> lawMapping.answer() == null
                        || (answer.isYes() && lawMapping.answer())
                        || (answer.isNo() && !lawMapping.answer()))
                .toList();
    }

    private static ComplianceStatus toComplianceStatus(SelfCheckAnswerValue answer) {
        return switch (answer) {
            case YES -> ComplianceStatus.COMPLIANT;
            case NO -> ComplianceStatus.NON_COMPLIANT;
            case NA -> ComplianceStatus.PENDING;
        };
    }

    @Transactional(readOnly = true)
    public List<AuditRegulationComplianceView> getMappings(Long userId, Long auditId) {
        auditRepository.findByIdAndUser_Id(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);

        return getMappings(auditId);
    }
}