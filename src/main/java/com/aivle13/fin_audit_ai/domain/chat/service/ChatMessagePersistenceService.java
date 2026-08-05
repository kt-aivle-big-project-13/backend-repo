package com.aivle13.fin_audit_ai.domain.chat.service;

import com.aivle13.fin_audit_ai.domain.chat.entity.ChatConversationEntity;
import com.aivle13.fin_audit_ai.domain.chat.entity.ChatMessageCitationEntity;
import com.aivle13.fin_audit_ai.domain.chat.entity.ChatMessageEntity;
import com.aivle13.fin_audit_ai.domain.chat.repository.ChatConversationRepository;
import com.aivle13.fin_audit_ai.domain.chat.repository.ChatMessageRepository;
import com.aivle13.fin_audit_ai.domain.chat.type.CitationType;
import com.aivle13.fin_audit_ai.domain.chat.type.GroundingStatus;
import com.aivle13.fin_audit_ai.global.ai.dto.chat.response.ChatAnswerResponse;
import com.aivle13.fin_audit_ai.global.exception.ai.AiServerErrorException;
import com.aivle13.fin_audit_ai.global.exception.chat.ChatConversationNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 질문·답변·인용 저장.
 *
 * <p>질문 처리 서비스와 분리한 이유는 두 가지다. 같은 클래스 안에서 호출하면 프록시를
 * 거치지 않아 {@code @Transactional} 이 적용되지 않고, AI 호출까지 트랜잭션에 묶으면
 * 응답을 기다리는 동안 커넥션을 붙잡게 된다. 저장만 짧게 묶는다.
 */
@Service
@RequiredArgsConstructor
public class ChatMessagePersistenceService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;

    @Transactional
    public ChatMessageEntity save(
            Long conversationId,
            String question,
            ChatAnswerResponse answer
    ) {
        ChatConversationEntity conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(ChatConversationNotFoundException::new);

        // 질문과 답변을 같은 트랜잭션에 넣어, 답변 없이 질문만 남는 상태를 막는다.
        messageRepository.save(
                ChatMessageEntity.question(conversation, question)
        );

        ChatMessageEntity answerMessage = ChatMessageEntity.answer(
                conversation,
                answer.answer(),
                GroundingStatus.from(answer.groundingStatus())
                        .orElseThrow(AiServerErrorException::new)
        );

        if (answer.citations() != null) {
            for (ChatAnswerResponse.Citation citation : answer.citations()) {
                answerMessage.addCitation(toCitation(citation));
            }
        }

        return messageRepository.save(answerMessage);
    }

    private ChatMessageCitationEntity toCitation(
            ChatAnswerResponse.Citation citation
    ) {
        return ChatMessageCitationEntity.of(
                CitationType.from(citation.type())
                        .orElseThrow(AiServerErrorException::new),
                citation.reference(),
                citation.value(),
                citation.similarity(),
                citation.sourceUrl()
        );
    }
}
