package com.aivle13.fin_audit_ai.domain.objection.service;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionDetailResponse;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionModelEvidenceResponse;
import com.aivle13.fin_audit_ai.domain.objection.entity.ObjectionEntity;
import com.aivle13.fin_audit_ai.domain.objection.repository.ObjectionRepository;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionStatus;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.LetterBodyResponse;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionDocumentResponse;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionDecision;
import com.aivle13.fin_audit_ai.global.exception.objection.ObjectionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ObjectionQueryServiceTest {

    @Mock
    private ObjectionRepository objectionRepository;

    @Mock
    private ObjectionModelEvidenceService
            objectionModelEvidenceService;
    @Mock
    private ObjectionLetterGenerationService
            objectionLetterGenerationService;

    @InjectMocks
    private ObjectionQueryService objectionQueryService;

    @Test
    void detailContainsCsvEvidenceAndGlobalModelEvidence() {
        ObjectionEntity objection = mock(ObjectionEntity.class);
        AiModelEntity model = mock(AiModelEntity.class);

        ObjectionModelEvidenceResponse evidence =
                new ObjectionModelEvidenceResponse(
                        1,
                        "EXT_SOURCE_2",
                        "외부 평가 지표 2",
                        new BigDecimal("0.136000"),
                        new BigDecimal("0.240000"),
                        "RISK_DECREASE"
                );

        when(objectionRepository.findByIdAndModel_User_Id(1L, 10L))
                .thenReturn(Optional.of(objection));
        when(objection.getModel()).thenReturn(model);
        when(objection.getStatus())
                .thenReturn(ObjectionStatus.DRAFT);
        when(objection.getShapEvidence())
                .thenReturn(
                        "부채비율 82%; 최근 연체 이력 2건/6개월"
                );
        when(objection.getStaffNote())
                .thenReturn(
                        "부채비율과 연체 이력을 종합 검토했습니다."
                );
        when(objectionModelEvidenceService
                .findLatestTopEvidence(model))
                .thenReturn(List.of(evidence));

        ObjectionDetailResponse result =
                objectionQueryService.getDetail(10L, 1L);

        assertThat(result.shapEvidence())
                .isEqualTo(
                        "부채비율 82%; 최근 연체 이력 2건/6개월"
                );
        assertThat(result.staffNote())
                .isEqualTo(
                        "부채비율과 연체 이력을 종합 검토했습니다."
                );
        assertThat(result.globalModelEvidence())
                .containsExactly(evidence);

        verify(objectionModelEvidenceService)
                .findLatestTopEvidence(model);
    }

    @Test
    void detailReturnsEmptyGlobalEvidenceWhenModelAuditIsUnavailable() {
        ObjectionEntity objection = mock(ObjectionEntity.class);
        AiModelEntity model = mock(AiModelEntity.class);

        when(objectionRepository.findByIdAndModel_User_Id(
                1L,
                10L
        )).thenReturn(Optional.of(objection));
        when(objection.getModel()).thenReturn(model);
        when(objection.getStatus())
                .thenReturn(ObjectionStatus.DRAFT);
        when(objectionModelEvidenceService
                .findLatestTopEvidence(model))
                .thenReturn(List.of());

        ObjectionDetailResponse result =
                objectionQueryService.getDetail(10L, 1L);

        assertThat(result.globalModelEvidence()).isEmpty();
    }

    @Test
    void documentUsesLlmWithGlobalModelEvidence() {
        ObjectionEntity objection = mock(ObjectionEntity.class);
        AiModelEntity model = mock(AiModelEntity.class);

        ObjectionModelEvidenceResponse evidence =
                new ObjectionModelEvidenceResponse(
                        1,
                        "EXT_SOURCE_2",
                        "외부 평가 지표 2",
                        new BigDecimal("0.136000"),
                        new BigDecimal("0.240000"),
                        "RISK_DECREASE"
                );

        when(objectionRepository.findByIdAndModel_User_Id(1L, 10L))
                .thenReturn(Optional.of(objection));
        when(objection.getStatus())
                .thenReturn(ObjectionStatus.DRAFT);
        when(objection.getModel()).thenReturn(model);
        when(objectionModelEvidenceService
                .findLatestTopEvidence(model))
                .thenReturn(List.of(evidence));
        when(objectionLetterGenerationService.generate(
                objection,
                ObjectionDecision.REJECT_MAINTAIN,
                List.of(evidence),
                false
        )).thenReturn("LLM이 생성한 최초 안내문");

        ObjectionDocumentResponse result =
                objectionQueryService.getDocument(
                        10L,
                        1L,
                        ObjectionDecision.REJECT_MAINTAIN
                );

        assertThat(result.letterBody())
                .isEqualTo("LLM이 생성한 최초 안내문");

        verify(objectionLetterGenerationService).generate(
                objection,
                ObjectionDecision.REJECT_MAINTAIN,
                List.of(evidence),
                false
        );
    }

    @Test
    void regenerationUsesLlmWithRegenerationFlag() {
        ObjectionEntity objection = mock(ObjectionEntity.class);
        AiModelEntity model = mock(AiModelEntity.class);

        when(objectionRepository.findByIdAndModel_User_Id(1L, 10L))
                .thenReturn(Optional.of(objection));
        when(objection.getStatus())
                .thenReturn(ObjectionStatus.DRAFT);
        when(objection.getModel()).thenReturn(model);
        when(objectionModelEvidenceService
                .findLatestTopEvidence(model))
                .thenReturn(List.of());
        when(objectionLetterGenerationService.generate(
                objection,
                ObjectionDecision.REEXAMINATION,
                List.of(),
                true
        )).thenReturn("LLM이 재생성한 안내문");

        LetterBodyResponse result =
                objectionQueryService.regenerateLetter(
                        10L,
                        1L,
                        ObjectionDecision.REEXAMINATION
                );

        assertThat(result.letterBody())
                .isEqualTo("LLM이 재생성한 안내문");

        verify(objectionLetterGenerationService).generate(
                objection,
                ObjectionDecision.REEXAMINATION,
                List.of(),
                true
        );
    }

    @Test
    void deliveredDocumentReturnsStoredDraftWithoutLlmCall() {
        ObjectionEntity objection = mock(ObjectionEntity.class);

        when(objectionRepository.findByIdAndModel_User_Id(1L, 10L))
                .thenReturn(Optional.of(objection));
        when(objection.getStatus())
                .thenReturn(ObjectionStatus.DELIVERED);
        when(objection.getDecision())
                .thenReturn(ObjectionDecision.REJECT_MAINTAIN);
        when(objection.getDraftContent())
                .thenReturn("발송 당시 확정된 안내문");

        ObjectionDocumentResponse result =
                objectionQueryService.getDocument(
                        10L,
                        1L,
                        ObjectionDecision.REEXAMINATION
                );

        assertThat(result.decision())
                .isEqualTo(ObjectionDecision.REJECT_MAINTAIN);
        assertThat(result.letterBody())
                .isEqualTo("발송 당시 확정된 안내문");

        verifyNoInteractions(
                objectionModelEvidenceService,
                objectionLetterGenerationService
        );
    }
    @Test
    void otherUserCannotReadObjectionDetail() {
        when(objectionRepository.findByIdAndModel_User_Id(
                1L,
                99L
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                objectionQueryService.getDetail(
                        99L,
                        1L
                )
        ).isInstanceOf(ObjectionNotFoundException.class);

        verifyNoInteractions(
                objectionModelEvidenceService,
                objectionLetterGenerationService
        );
    }

    @Test
    void otherUserCannotGenerateOrRegenerateDocument() {
        when(objectionRepository.findByIdAndModel_User_Id(
                1L,
                99L
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                objectionQueryService.getDocument(
                        99L,
                        1L,
                        ObjectionDecision.REJECT_MAINTAIN
                )
        ).isInstanceOf(ObjectionNotFoundException.class);

        assertThatThrownBy(() ->
                objectionQueryService.regenerateLetter(
                        99L,
                        1L,
                        ObjectionDecision.REJECT_MAINTAIN
                )
        ).isInstanceOf(ObjectionNotFoundException.class);

        verifyNoInteractions(
                objectionModelEvidenceService,
                objectionLetterGenerationService
        );
    }
}