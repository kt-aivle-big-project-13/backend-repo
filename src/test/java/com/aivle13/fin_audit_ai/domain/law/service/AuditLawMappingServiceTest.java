package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditLawMappingEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.SelfCheckItemCode;
import com.aivle13.fin_audit_ai.domain.law.dto.AuditLawComplianceView;
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
    void derivesComplianceFromAnswerAndDedupesAcrossItems() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID)).willReturn(Optional.of(audit));

        SelfCheckAnswerEntity supervisionAnswer = answer(SelfCheckItemCode.SUPERVISION, true);
        SelfCheckAnswerEntity riskAnswer = answer(SelfCheckItemCode.RISK_MANAGEMENT, false);
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(supervisionAnswer, riskAnswer));

        LawArticleEntity sharedArticle = article(1L);
        LawArticleEntity onlyFromRisk = article(2L);

        given(lawArticleSearchService.searchSimilarArticles(
                SelfCheckItemCode.SUPERVISION.label(), 5))
                .willReturn(List.of(sharedArticle));
        given(lawArticleSearchService.searchSimilarArticles(
                SelfCheckItemCode.RISK_MANAGEMENT.label(), 5))
                .willReturn(List.of(sharedArticle, onlyFromRisk));

        auditLawMappingService.mapFromSelfCheckAnswers(AUDIT_ID);

        verify(auditLawMappingRepository).deleteAllByAudit_Id(AUDIT_ID);

        ArgumentCaptor<Collection<AuditLawMappingEntity>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(auditLawMappingRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(2);

        // 먼저 처리된 항목(SUPERVISION, 답변 '예')이 매핑을 선점하므로, 두 항목 모두에서
        // 검색되는 sharedArticle은 COMPLIANT로 남는다.
        assertThat(captor.getValue())
                .filteredOn(mapping -> mapping.getArticle().getId().equals(1L))
                .singleElement()
                .satisfies(mapping -> {
                    assertThat(mapping.getCompliance()).isEqualTo(ComplianceStatus.COMPLIANT);
                    assertThat(mapping.getEvidence()).contains("답변: 예");
                });
        assertThat(captor.getValue())
                .filteredOn(mapping -> mapping.getArticle().getId().equals(2L))
                .singleElement()
                .satisfies(mapping -> {
                    assertThat(mapping.getCompliance()).isEqualTo(ComplianceStatus.NON_COMPLIANT);
                    assertThat(mapping.getEvidence()).contains("답변: 아니요");
                });
    }

    @Test
    void regenerationDeletesAllPreviousMappingsRegardlessOfCompliance() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID)).willReturn(Optional.of(audit));

        SelfCheckAnswerEntity supervisionAnswer = answer(SelfCheckItemCode.SUPERVISION, false);
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of(supervisionAnswer));

        LawArticleEntity article = article(1L);
        given(lawArticleSearchService.searchSimilarArticles(eq(SelfCheckItemCode.SUPERVISION.label()), eq(5)))
                .willReturn(List.of(article));

        auditLawMappingService.mapFromSelfCheckAnswers(AUDIT_ID);

        // 이전에 어떤 compliance로 저장돼 있었든 재생성 시 전부 지우고 새로 채운다 —
        // 사람이 확정한 값이라 보존해야 한다는 개념 자체가 없다.
        verify(auditLawMappingRepository).deleteAllByAudit_Id(AUDIT_ID);

        ArgumentCaptor<Collection<AuditLawMappingEntity>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(auditLawMappingRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .singleElement()
                .satisfies(mapping -> assertThat(mapping.getCompliance()).isEqualTo(ComplianceStatus.NON_COMPLIANT));
    }

    @Test
    void getMappingsReturnsAllMappingsForAudit() {
        LawArticleEntity article = article(1L);
        AuditLawMappingEntity mapping = AuditLawMappingEntity.of(
                audit, article, ComplianceStatus.NON_COMPLIANT, "자율점검 기반 자동 매칭"
        );
        given(auditLawMappingRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(List.of(mapping));

        List<AuditLawComplianceView> views = auditLawMappingService.getMappings(AUDIT_ID);

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.articleNumber()).isEqualTo("제1조");
            assertThat(view.articleTitle()).isEqualTo("인공지능 기본법");
            assertThat(view.compliance()).isEqualTo(ComplianceStatus.NON_COMPLIANT);
            assertThat(view.evidence()).isEqualTo("자율점검 기반 자동 매칭");
        });
    }

    @Test
    void throwsWhenAuditDoesNotExist() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID)).willReturn(Optional.empty());

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
