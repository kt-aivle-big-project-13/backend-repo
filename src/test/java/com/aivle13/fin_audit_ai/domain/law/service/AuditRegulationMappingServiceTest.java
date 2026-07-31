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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditRegulationMappingServiceTest {

    private static final Long AUDIT_ID = 21L;
    private static final Long USER_ID = 7L;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private SelfCheckAnswerRepository selfCheckAnswerRepository;

    @Mock
    private AuditRegulationMappingRepository auditRegulationMappingRepository;

    @Mock
    private LawArticleRepository lawArticleRepository;

    @Mock
    private AuditEntity audit;

    @InjectMocks
    private AuditRegulationMappingService auditRegulationMappingService;

    @Test
    void derivesComplianceFromAnswerAndDedupesAcrossItems() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID)).willReturn(Optional.of(audit));

        // OVERSIGHT(예)와 RISK_MANAGEMENT(아니요) 둘 다 제34조를 참조한다(문항마다 다른 항으로).
        // 같은 조항이 두 항목에 걸쳐 중복 매핑되지 않는지 검증한다.
        SelfCheckAnswerEntity oversightAnswer = answer(SelfCheckItemCode.OVERSIGHT, true);
        SelfCheckAnswerEntity riskAnswer = answer(SelfCheckItemCode.RISK_MANAGEMENT, false);
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(oversightAnswer, riskAnswer));

        LawArticleEntity sharedArticle = article(1L);
        given(lawArticleRepository.findByLawNameAndArticleNo("AI 기본법", "제34조"))
                .willReturn(Optional.of(sharedArticle));
        given(lawArticleRepository.findByLawNameAndArticleNo("AI 기본법", "제32조"))
                .willReturn(Optional.of(article(2L)));
        given(lawArticleRepository.findByLawNameAndArticleNo("AI 기본법 시행령", "제27조"))
                .willReturn(Optional.of(article(3L)));

        auditRegulationMappingService.mapFromSelfCheckAnswers(AUDIT_ID);

        verify(auditRegulationMappingRepository).deleteAllByAudit_Id(AUDIT_ID);

        ArgumentCaptor<Collection<AuditRegulationMappingEntity>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(auditRegulationMappingRepository).saveAll(captor.capture());

        // OVERSIGHT(제34조 1개) + RISK_MANAGEMENT(제32조, 시행령제27조, 제34조 3개) - 중복 1개 = 3개
        assertThat(captor.getValue()).hasSize(3);

        // 먼저 처리된 항목(OVERSIGHT, 답변 '예')이 매핑을 선점하므로, 두 항목 모두에서
        // 참조되는 제34조는 COMPLIANT로 남는다.
        assertThat(captor.getValue())
                .filteredOn(mapping -> mapping.getArticle().getId().equals(1L))
                .singleElement()
                .satisfies(mapping -> {
                    assertThat(mapping.getCompliance()).isEqualTo(ComplianceStatus.COMPLIANT);
                    assertThat(mapping.getEvidence()).contains("답변: 예");
                });
        assertThat(captor.getValue())
                .filteredOn(mapping -> mapping.getArticle().getId().equals(3L))
                .singleElement()
                .satisfies(mapping -> {
                    assertThat(mapping.getCompliance()).isEqualTo(ComplianceStatus.NON_COMPLIANT);
                    assertThat(mapping.getEvidence()).contains("답변: 아니요");
                });
    }

    @Test
    void noticeExcludesPenaltyArticleWhenCompliant() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID)).willReturn(Optional.of(audit));

        SelfCheckAnswerEntity compliantNotice = answer(SelfCheckItemCode.NOTICE, true);
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(compliantNotice));

        given(lawArticleRepository.findByLawNameAndArticleNo("AI 기본법", "제31조"))
                .willReturn(Optional.of(article(1L)));
        given(lawArticleRepository.findByLawNameAndArticleNo("AI 기본법 시행령", "제23조"))
                .willReturn(Optional.of(article(2L)));

        auditRegulationMappingService.mapFromSelfCheckAnswers(AUDIT_ID);

        ArgumentCaptor<Collection<AuditRegulationMappingEntity>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(auditRegulationMappingRepository).saveAll(captor.capture());

        // '예'일 때는 과태료(제43조)가 answer=false 전용이라 조회 자체가 일어나지 않는다.
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue())
                .noneMatch(mapping -> "제43조".equals(mapping.getArticle().getArticleNo()));
    }

    @Test
    void noticeNonCompliantIncludesPenaltyArticle() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID)).willReturn(Optional.of(audit));

        SelfCheckAnswerEntity nonCompliantNotice = answer(SelfCheckItemCode.NOTICE, false);
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(nonCompliantNotice));

        given(lawArticleRepository.findByLawNameAndArticleNo("AI 기본법", "제31조"))
                .willReturn(Optional.of(article(1L)));
        given(lawArticleRepository.findByLawNameAndArticleNo("AI 기본법 시행령", "제23조"))
                .willReturn(Optional.of(article(2L)));
        given(lawArticleRepository.findByLawNameAndArticleNo("AI 기본법", "제43조"))
                .willReturn(Optional.of(article(43L)));

        auditRegulationMappingService.mapFromSelfCheckAnswers(AUDIT_ID);

        ArgumentCaptor<Collection<AuditRegulationMappingEntity>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(auditRegulationMappingRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(3);
        assertThat(captor.getValue())
                .anyMatch(mapping -> "제43조".equals(mapping.getArticle().getArticleNo())
                        && mapping.getCompliance() == ComplianceStatus.NON_COMPLIANT);
    }

    @Test
    void throwsWhenMappedArticleMissingFromLawArticles() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID)).willReturn(Optional.of(audit));

        SelfCheckAnswerEntity answer = answer(SelfCheckItemCode.DOCUMENTATION, true);
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(List.of(answer));

        given(lawArticleRepository.findByLawNameAndArticleNo("AI 기본법", "제34조"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> auditRegulationMappingService.mapFromSelfCheckAnswers(AUDIT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI 기본법")
                .hasMessageContaining("제34조");
    }

    @Test
    void regenerationDeletesAllPreviousMappingsRegardlessOfCompliance() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID)).willReturn(Optional.of(audit));

        SelfCheckAnswerEntity documentationAnswer = answer(SelfCheckItemCode.DOCUMENTATION, false);
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(documentationAnswer));

        given(lawArticleRepository.findByLawNameAndArticleNo("AI 기본법", "제34조"))
                .willReturn(Optional.of(article(1L)));
        given(lawArticleRepository.findByLawNameAndArticleNo("AI 기본법 시행령", "제27조"))
                .willReturn(Optional.of(article(2L)));

        auditRegulationMappingService.mapFromSelfCheckAnswers(AUDIT_ID);

        // 이전에 어떤 compliance로 저장돼 있었든 재생성 시 전부 지우고 새로 채운다 —
        // 사람이 확정한 값이라 보존해야 한다는 개념 자체가 없다.
        verify(auditRegulationMappingRepository).deleteAllByAudit_Id(AUDIT_ID);

        ArgumentCaptor<Collection<AuditRegulationMappingEntity>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(auditRegulationMappingRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .hasSize(2)
                .allSatisfy(mapping -> assertThat(mapping.getCompliance()).isEqualTo(ComplianceStatus.NON_COMPLIANT));
    }

    @Test
    void getMappingsReturnsAllMappingsForAudit() {
        LawArticleEntity article = article(1L);
        AuditRegulationMappingEntity mapping = AuditRegulationMappingEntity.of(
                audit, article, ComplianceStatus.NON_COMPLIANT, "자율점검 기반 자동 매칭"
        );
        given(auditRegulationMappingRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(List.of(mapping));

        List<AuditRegulationComplianceView> views = auditRegulationMappingService.getMappings(AUDIT_ID);

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.articleNumber()).isEqualTo("제1조");
            assertThat(view.articleTitle()).isEqualTo("인공지능 기본법");
            assertThat(view.content()).isEqualTo("조문 원문");
            assertThat(view.compliance()).isEqualTo(ComplianceStatus.NON_COMPLIANT);
            assertThat(view.evidence()).isEqualTo("자율점검 기반 자동 매칭");
            assertThat(view.matchedItems()).isEmpty();
        });
    }

    @Test
    void getMappingsResolvesMatchedItemsFromSelfCheckAnswers() {
        // 제31조는 ARTICLE_MAPPING상 NOTICE 항목의 ①항이다. audit_law_mappings에는 문항 정보가
        // 저장돼 있지 않으므로, 자율점검 답변으로 조회 시점에 역산되는지 검증한다.
        LawArticleEntity article = LawArticleEntity.of(
                "AI 기본법", "제31조", "조문 원문", LocalDate.of(2026, 1, 22)
        );
        setId(article, 1L);
        AuditRegulationMappingEntity mapping = AuditRegulationMappingEntity.of(
                audit, article, ComplianceStatus.COMPLIANT, "자율점검 기반 자동 매칭"
        );
        given(auditRegulationMappingRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(List.of(mapping));
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(answer(SelfCheckItemCode.NOTICE, true)));

        List<AuditRegulationComplianceView> views = auditRegulationMappingService.getMappings(AUDIT_ID);

        assertThat(views).singleElement()
                .satisfies(view -> assertThat(view.matchedItems())
                        .containsExactly(new MatchedChecklistItem(SelfCheckItemCode.NOTICE, "①")));
    }

    @Test
    void getMappingsReturnsDifferentClauseNoForSameArticleAcrossItems() {
        // 제34조는 문항마다 다른 항으로 걸린다: RISK_MANAGEMENT는 ①1호, OVERSIGHT는 ①4호,
        // DOCUMENTATION은 ①5호 — 조 하나에 라벨 하나만 붙이는 구조로는 표현 불가능했던 케이스.
        LawArticleEntity article = LawArticleEntity.of(
                "AI 기본법", "제34조", "조문 원문", LocalDate.of(2026, 1, 22)
        );
        setId(article, 1L);
        AuditRegulationMappingEntity mapping = AuditRegulationMappingEntity.of(
                audit, article, ComplianceStatus.COMPLIANT, "자율점검 기반 자동 매칭"
        );
        given(auditRegulationMappingRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(List.of(mapping));
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(List.of(
                answer(SelfCheckItemCode.RISK_MANAGEMENT, true),
                answer(SelfCheckItemCode.OVERSIGHT, true),
                answer(SelfCheckItemCode.DOCUMENTATION, true)
        ));

        List<AuditRegulationComplianceView> views = auditRegulationMappingService.getMappings(AUDIT_ID);

        assertThat(views).singleElement()
                .satisfies(view -> assertThat(view.matchedItems()).containsExactlyInAnyOrder(
                        new MatchedChecklistItem(SelfCheckItemCode.RISK_MANAGEMENT, "①1호"),
                        new MatchedChecklistItem(SelfCheckItemCode.OVERSIGHT, "①4호"),
                        new MatchedChecklistItem(SelfCheckItemCode.DOCUMENTATION, "①5호")
                ));
    }

    @Test
    void getMappingsLeavesMatchedItemsEmptyWhenArticleNotInAnyMapping() {
        // 제1조는 ARTICLE_MAPPING 어디에도 없는 조항이라 자율점검 답변이 있어도 매칭이 없어야 한다.
        LawArticleEntity article = article(1L);
        AuditRegulationMappingEntity mapping = AuditRegulationMappingEntity.of(
                audit, article, ComplianceStatus.COMPLIANT, "자율점검 기반 자동 매칭"
        );
        given(auditRegulationMappingRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(List.of(mapping));
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(answer(SelfCheckItemCode.NOTICE, true)));

        List<AuditRegulationComplianceView> views = auditRegulationMappingService.getMappings(AUDIT_ID);

        assertThat(views).singleElement()
                .satisfies(view -> assertThat(view.matchedItems()).isEmpty());
    }

    @Test
    void getMappingsForUserReturnsMappingsWhenAuditOwnedByUser() {
        LawArticleEntity article = article(1L);
        AuditRegulationMappingEntity mapping = AuditRegulationMappingEntity.of(
                audit, article, ComplianceStatus.COMPLIANT, "자율점검 기반 자동 매칭"
        );
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID)).willReturn(Optional.of(audit));
        given(auditRegulationMappingRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(List.of(mapping));

        List<AuditRegulationComplianceView> views = auditRegulationMappingService.getMappings(USER_ID, AUDIT_ID);

        assertThat(views).singleElement()
                .satisfies(view -> assertThat(view.compliance()).isEqualTo(ComplianceStatus.COMPLIANT));
    }

    @Test
    void getMappingsForUserThrowsWhenAuditNotOwnedByUser() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> auditRegulationMappingService.getMappings(USER_ID, AUDIT_ID))
                .isInstanceOf(AuditNotFoundException.class);
    }

    @Test
    void throwsWhenAuditDoesNotExist() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> auditRegulationMappingService.mapFromSelfCheckAnswers(AUDIT_ID))
                .isInstanceOf(AuditNotFoundException.class);
    }

    private SelfCheckAnswerEntity answer(SelfCheckItemCode itemCode, boolean value) {
        return SelfCheckAnswerEntity.of(audit, itemCode, value);
    }

    private LawArticleEntity article(Long id) {
        LawArticleEntity article = LawArticleEntity.of(
                "인공지능 기본법", "제" + id + "조", "조문 원문", LocalDate.of(2026, 1, 22)
        );
        setId(article, id);
        return article;
    }

    // LawArticleEntity.id는 @GeneratedValue라 정적 팩토리로 채울 수 없어 리플렉션으로 주입한다.
    private void setId(LawArticleEntity article, Long id) {
        try {
            var field = LawArticleEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(article, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
