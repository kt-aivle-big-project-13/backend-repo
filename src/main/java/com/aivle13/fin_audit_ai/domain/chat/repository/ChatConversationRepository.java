package com.aivle13.fin_audit_ai.domain.chat.repository;

import com.aivle13.fin_audit_ai.domain.chat.entity.ChatConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository
        extends JpaRepository<ChatConversationEntity, Long> {

    List<ChatConversationEntity>
    findAllByAudit_IdAndUser_IdOrderByCreatedAtDescIdDesc(
            Long auditId,
            Long userId
    );

    // 대화 접근은 항상 소유자 기준으로만 허용한다.
    Optional<ChatConversationEntity> findByIdAndUser_Id(
            Long conversationId,
            Long userId
    );
}
