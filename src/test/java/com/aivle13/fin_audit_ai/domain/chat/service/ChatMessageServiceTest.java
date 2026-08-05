package com.aivle13.fin_audit_ai.domain.chat.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.chat.dto.response.ChatMessageResponse;
import com.aivle13.fin_audit_ai.domain.chat.entity.ChatConversationEntity;
import com.aivle13.fin_audit_ai.domain.chat.entity.ChatMessageCitationEntity;
import com.aivle13.fin_audit_ai.domain.chat.entity.ChatMessageEntity;
import com.aivle13.fin_audit_ai.domain.chat.repository.ChatConversationRepository;
import com.aivle13.fin_audit_ai.domain.chat.repository.ChatMessageRepository;
import com.aivle13.fin_audit_ai.domain.chat.type.CitationType;
import com.aivle13.fin_audit_ai.domain.chat.type.GroundingStatus;
import com.aivle13.fin_audit_ai.global.ai.client.chat.ChatAnswerClient;
import com.aivle13.fin_audit_ai.global.ai.dto.chat.request.ChatAnswerRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.chat.response.ChatAnswerResponse;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.exception.ai.AiServerErrorException;
import com.aivle13.fin_audit_ai.global.exception.chat.ChatConversationNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    private static final Long USER_ID = 2L;
    private static final Long AUDIT_ID = 42L;
    private static final Long CONVERSATION_ID = 7L;
    private static final String QUESTION = "AGE_GROUP의 승인율 격차가 큰가요?";

    @Mock
    private ChatConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private ChatFactAssembler factAssembler;

    @Mock
    private ChatLawSearchService lawSearchService;

    @Mock
    private ChatReportSectionLoader reportSectionLoader;

    @Mock
    private ChatMessagePersistenceService persistenceService;

    @Mock
    private ChatAnswerClient chatAnswerClient;

    @Mock
    private ChatRateLimiter rateLimiter;

    @Mock
    private ChatConversationEntity conversation;

    @Mock
    private AuditEntity audit;

    @InjectMocks
    private ChatMessageService service;

    @Test
    void sendsAssembledFactsToAiServer() {
        givenConversation();

        List<ChatAnswerRequest.AuditFact> facts = List.of(
                new ChatAnswerRequest.AuditFact(
                        "AUDIT_METRIC",
                        "EQUAL_OPPORTUNITY / AGE_GROUP",
                        "0.1123",
                        "임계값 0.2000, 상태 PASS"
                )
        );

        List<ChatAnswerRequest.LawArticle> lawArticles = List.of(
                new ChatAnswerRequest.LawArticle(
                        "신용정보법",
                        "제36조의2",
                        "자동화 평가 결과 설명·이의제기 보장",
                        "조항 본문",
                        new java.math.BigDecimal("0.8300"),
                        null,
                        false
                )
        );

        List<ChatAnswerRequest.ReportSection> reportSections = List.of(
                new ChatAnswerRequest.ReportSection(
                        "BIAS_REPORT",
                        "metric_results",
                        "5. 공정성 지표 결과",
                        "AGE_GROUP 의 Equal Opportunity Difference 는 0.1123 으로 확인됨"
                )
        );

        given(factAssembler.assemble(AUDIT_ID)).willReturn(facts);
        given(lawSearchService.search(QUESTION)).willReturn(lawArticles);
        given(reportSectionLoader.load(AUDIT_ID)).willReturn(reportSections);
        givenAnswer(groundedAnswer());
        givenSavedMessage();

        service.ask(USER_ID, CONVERSATION_ID, QUESTION);

        ArgumentCaptor<ChatAnswerRequest> captor =
                ArgumentCaptor.forClass(ChatAnswerRequest.class);

        verify(chatAnswerClient).generate(captor.capture());

        ChatAnswerRequest request = captor.getValue();

        assertThat(request.auditId()).isEqualTo(AUDIT_ID);
        assertThat(request.question()).isEqualTo(QUESTION);
        assertThat(request.auditFacts()).isEqualTo(facts);
        assertThat(request.reportSections()).isEqualTo(reportSections);

        assertThat(request.lawArticles()).isEqualTo(lawArticles);
    }

    @Test
    void returnsAnswerWithCitations() {
        givenConversation();
        given(factAssembler.assemble(AUDIT_ID)).willReturn(List.of());
        given(lawSearchService.search(QUESTION)).willReturn(List.of());
        givenAnswer(groundedAnswer());
        givenSavedMessage();

        ChatMessageResponse response =
                service.ask(USER_ID, CONVERSATION_ID, QUESTION);

        assertThat(response.groundingStatus())
                .isEqualTo(GroundingStatus.GROUNDED);
        assertThat(response.citations()).hasSize(1);
        assertThat(response.citations().get(0).reference())
                .isEqualTo("EQUAL_OPPORTUNITY / AGE_GROUP");
    }

    @Test
    void checksRateLimitBeforeCallingAiServer() {
        givenConversation();

        willThrow(new BusinessException(ErrorCode.TOO_MANY_REQUESTS))
                .given(rateLimiter)
                .checkAndIncrease(USER_ID, AUDIT_ID);

        assertThatThrownBy(() ->
                service.ask(USER_ID, CONVERSATION_ID, QUESTION)
        )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode",
                        ErrorCode.TOO_MANY_REQUESTS
                );

        // 제한에 걸리면 LLM 을 호출하지도, 저장하지도 않는다.
        verify(chatAnswerClient, never()).generate(any());
        verify(persistenceService, never())
                .save(anyLong(), anyString(), any());
    }

    @Test
    void rejectsConversationOfAnotherUser() {
        given(conversationRepository.findByIdAndUser_Id(
                CONVERSATION_ID,
                USER_ID
        ))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.ask(USER_ID, CONVERSATION_ID, QUESTION)
        ).isInstanceOf(ChatConversationNotFoundException.class);

        verify(chatAnswerClient, never()).generate(any());
    }

    @Test
    void rejectsBlankAnswerFromAiServer() {
        givenConversation();
        given(factAssembler.assemble(AUDIT_ID)).willReturn(List.of());
        given(lawSearchService.search(QUESTION)).willReturn(List.of());

        givenAnswer(new ChatAnswerResponse(
                AUDIT_ID,
                "  ",
                List.of(),
                "GROUNDED",
                "2026-08-01T10:00:00Z"
        ));

        assertThatThrownBy(() ->
                service.ask(USER_ID, CONVERSATION_ID, QUESTION)
        ).isInstanceOf(AiServerErrorException.class);

        verify(persistenceService, never())
                .save(anyLong(), anyString(), any());
    }

    private void givenConversation() {
        given(conversationRepository.findByIdAndUser_Id(
                CONVERSATION_ID,
                USER_ID
        ))
                .willReturn(Optional.of(conversation));

        given(conversation.getAudit()).willReturn(audit);
        given(audit.getId()).willReturn(AUDIT_ID);
    }

    private void givenAnswer(ChatAnswerResponse answer) {
        given(chatAnswerClient.generate(any(ChatAnswerRequest.class)))
                .willReturn(answer);
    }

    private ChatAnswerResponse groundedAnswer() {
        return new ChatAnswerResponse(
                AUDIT_ID,
                "AGE_GROUP 의 Equal Opportunity Difference 는 0.1123 입니다[1].",
                List.of(new ChatAnswerResponse.Citation(
                        "AUDIT_METRIC",
                        "EQUAL_OPPORTUNITY / AGE_GROUP",
                        "0.1123",
                        null,
                        null,
                        false
                )),
                "GROUNDED",
                "2026-08-01T10:00:00Z"
        );
    }

    private void givenSavedMessage() {
        ChatMessageEntity saved = ChatMessageEntity.answer(
                conversation,
                "AGE_GROUP 의 Equal Opportunity Difference 는 0.1123 입니다[1].",
                GroundingStatus.GROUNDED
        );

        saved.addCitation(ChatMessageCitationEntity.of(
                CitationType.AUDIT_METRIC,
                "EQUAL_OPPORTUNITY / AGE_GROUP",
                "0.1123",
                null,
                null
        ));

        given(persistenceService.save(
                eq(CONVERSATION_ID),
                eq(QUESTION),
                any(ChatAnswerResponse.class)
        )).willReturn(saved);
    }
}
