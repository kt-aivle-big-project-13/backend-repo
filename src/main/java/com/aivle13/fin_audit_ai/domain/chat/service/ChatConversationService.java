package com.aivle13.fin_audit_ai.domain.chat.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.chat.dto.response.ChatConversationResponse;
import com.aivle13.fin_audit_ai.domain.chat.entity.ChatConversationEntity;
import com.aivle13.fin_audit_ai.domain.chat.repository.ChatConversationRepository;
import com.aivle13.fin_audit_ai.global.exception.chat.ChatConversationNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 질의 대화의 생성·목록·삭제.
 *
 * <p>권한은 감사 소유자 기준으로만 본다. 역할 기반 접근은 역할 체계가 확정된 뒤 별도로
 * 얹는다(model-repo `docs/04-chatbot-plan.md`).
 */
@Service
@RequiredArgsConstructor
public class ChatConversationService {

    private static final String DEFAULT_TITLE = "감사 결과 질의";

    private final AuditRepository auditRepository;
    private final ChatConversationRepository conversationRepository;

    @Transactional
    public ChatConversationResponse create(
            Long userId,
            Long auditId,
            String title
    ) {
        AuditEntity audit = auditRepository
                .findByIdAndUser_Id(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);

        ChatConversationEntity conversation = ChatConversationEntity.create(
                audit,
                audit.getUser(),
                resolveTitle(title)
        );

        return ChatConversationResponse.from(
                conversationRepository.save(conversation)
        );
    }

    @Transactional(readOnly = true)
    public List<ChatConversationResponse> getAll(
            Long userId,
            Long auditId
    ) {
        auditRepository.findByIdAndUser_Id(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);

        return conversationRepository
                .findAllByAudit_IdAndUser_IdOrderByCreatedAtDescIdDesc(
                        auditId,
                        userId
                )
                .stream()
                .map(ChatConversationResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long conversationId) {
        ChatConversationEntity conversation = conversationRepository
                .findByIdAndUser_Id(conversationId, userId)
                .orElseThrow(ChatConversationNotFoundException::new);

        conversationRepository.delete(conversation);
    }

    private String resolveTitle(String title) {
        return title == null || title.isBlank() ? DEFAULT_TITLE : title.trim();
    }
}
