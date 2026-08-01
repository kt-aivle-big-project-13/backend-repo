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
import com.aivle13.fin_audit_ai.global.ai.client.ChatAnswerClient;
import com.aivle13.fin_audit_ai.global.ai.dto.ChatAnswerRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.ChatAnswerResponse;
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

        given(factAssembler.assemble(AUDIT_ID)).willReturn(facts);
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

        // 2단계는 감사 수치만 근거로 쓴다. 법령·리포트는 후속 단계에서 채운다.
        assertThat(request.lawArticles()).isEmpty();
        assertThat(request.reportSections()).isEmpty();
    }

    @Test
    void returnsAnswerWithCitations() {
        givenConversation();
        given(factAssembler.assemble(AUDIT_ID)).willReturn(List.of());
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
