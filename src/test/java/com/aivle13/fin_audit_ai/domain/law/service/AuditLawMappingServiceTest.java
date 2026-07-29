package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditLawMappingEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.SelfCheckItemCode;
import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.AuditLawMappingRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLawMappingServiceTest {

    private static final Long AUDIT_ID = 21L;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private SelfCheckAnswerRepository selfCheckAnswerRepository;

    @Mock
    private AuditLawMappingRepository auditLawMappingRepository;

    @Mock
    private LawArticleSearchService lawArticleSearchService;

    @Mock
    private AuditEntity audit;

    @InjectMocks
    private AuditLawMappingService auditLawMappingService;

    @Test
    void createsPendingMappingsDedupedAcrossItems() {
        given(auditRepository.findById(AUDIT_ID)).willReturn(Optional.of(audit));

        SelfCheckAnswerEntity supervisionAnswer = answer(SelfCheckItemCode.SUPERVISION, true);
        SelfCheckAnswerEntity riskAnswer = answer(SelfCheckItemCode.RISK_MANAGEMENT, false);
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(supervisionAnswer, riskAnswer));

        given(auditLawMappingRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(List.of());

        LawArticleEntity sharedArticle = article(1L);
        LawArticleEntity onlyFromRisk = article(2L);

        given(lawArticleSearchService.searchSimilarArticles(
                SelfCheckItemCode.SUPERVISION.label(), 5))
                .willReturn(List.of(sharedArticle));
        given(lawArticleSearchService.searchSimilarArticles(
                SelfCheckItemCode.RISK_MANAGEMENT.label(), 5))
                .willReturn(List.of(sharedArticle, onlyFromRisk));

        auditLawMappingService.mapFromSelfCheckAnswers(AUDIT_ID);

        verify(auditLawMappingRepository)
                .deleteAllByAudit_IdAndCompliance(AUDIT_ID, ComplianceStatus.PENDING);

        ArgumentCaptor<Collection<AuditLawMappingEntity>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(auditLawMappingRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue())
                .allSatisfy(mapping -> assertThat(mapping.getCompliance()).isEqualTo(ComplianceStatus.PENDING));

        // 먼저 처리된 항목(SUPERVISION, 답변 '예')이 매핑을 선점하므로, 두 항목 모두에서
        // 검색되는 sharedArticle의 근거 문구에는 '예'가 남는다.
        assertThat(captor.getValue())
                .filteredOn(mapping -> mapping.getArticle().getId().equals(1L))
                .singleElement()
                .satisfies(mapping -> assertThat(mapping.getEvidence()).contains("답변: 예"));
        assertThat(captor.getValue())
                .filteredOn(mapping -> mapping.getArticle().getId().equals(2L))
                .singleElement()
                .satisfies(mapping -> assertThat(mapping.getEvidence()).contains("답변: 아니요"));
    }

    @Test
    void skipsArticlesAlreadyDecidedByHuman() {
        given(auditRepository.findById(AUDIT_ID)).willReturn(Optional.of(audit));

        SelfCheckAnswerEntity supervisionAnswer = answer(SelfCheckItemCode.SUPERVISION, true);
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(supervisionAnswer));

        LawArticleEntity decidedArticle = article(1L);
        AuditLawMappingEntity confirmedMapping = AuditLawMappingEntity.of(
                audit, decidedArticle, ComplianceStatus.COMPLIANT, "이미 검토됨"
        );
        given(auditLawMappingRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(confirmedMapping));

        given(lawArticleSearchService.searchSimilarArticles(eq(SelfCheckItemCode.SUPERVISION.label()), eq(5)))
                .willReturn(List.of(decidedArticle));

        auditLawMappingService.mapFromSelfCheckAnswers(AUDIT_ID);

        ArgumentCaptor<Collection<AuditLawMappingEntity>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(auditLawMappingRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void throwsWhenAuditDoesNotExist() {
        given(auditRepository.findById(AUDIT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> auditLawMappingService.mapFromSelfCheckAnswers(AUDIT_ID))
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
