package com.aivle13.fin_audit_ai.domain.chat.service;

import com.aivle13.fin_audit_ai.domain.chat.dto.response.ChatMessageResponse;
import com.aivle13.fin_audit_ai.domain.chat.entity.ChatConversationEntity;
import com.aivle13.fin_audit_ai.domain.chat.repository.ChatConversationRepository;
import com.aivle13.fin_audit_ai.domain.chat.repository.ChatMessageRepository;
import com.aivle13.fin_audit_ai.global.ai.client.ChatAnswerClient;
import com.aivle13.fin_audit_ai.global.ai.dto.ChatAnswerRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.ChatAnswerResponse;
import com.aivle13.fin_audit_ai.global.exception.ai.AiServerErrorException;
import com.aivle13.fin_audit_ai.global.exception.chat.ChatConversationNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 질문 처리와 대화 이력 조회.
 *
 * <p>흐름은 근거 조립 → AI 호출 → 저장이다. 조립과 저장은 각각 짧은 트랜잭션으로 끝내고,
 * AI 호출은 트랜잭션 밖에서 한다.
 */
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatFactAssembler factAssembler;
    private final ChatLawSearchService lawSearchService;
    private final ChatMessagePersistenceService persistenceService;
    private final ChatAnswerClient chatAnswerClient;
    private final ChatRateLimiter rateLimiter;

    public ChatMessageResponse ask(
            Long userId,
            Long conversationId,
            String question
    ) {
        ChatConversationEntity conversation = findConversation(
                userId,
                conversationId
        );

        Long auditId = conversation.getAudit().getId();

        // 질문 한 건마다 LLM 을 호출하므로 저장·호출 전에 먼저 막는다.
        rateLimiter.checkAndIncrease(userId, auditId);

        // 리포트 서술은 5단계에서 채운다.
        ChatAnswerResponse answer = chatAnswerClient.generate(
                new ChatAnswerRequest(
                        auditId,
                        question,
                        factAssembler.assemble(auditId),
                        lawSearchService.search(question),
                        List.of()
                )
        );

        validateAnswer(answer);

        return ChatMessageResponse.from(
                persistenceService.save(conversationId, question, answer)
        );
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(
            Long userId,
            Long conversationId
    ) {
        findConversation(userId, conversationId);

        return messageRepository
                .findAllByConversationIdWithCitations(conversationId)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    private ChatConversationEntity findConversation(
            Long userId,
            Long conversationId
    ) {
        return conversationRepository
                .findByIdAndUser_Id(conversationId, userId)
                .orElseThrow(ChatConversationNotFoundException::new);
    }

    private void validateAnswer(ChatAnswerResponse answer) {
        if (answer == null
                || answer.answer() == null
                || answer.answer().isBlank()
                || answer.groundingStatus() == null) {
            throw new AiServerErrorException();
        }
    }
}
