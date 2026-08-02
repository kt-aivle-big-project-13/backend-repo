package com.aivle13.fin_audit_ai.domain.chat.repository;

import com.aivle13.fin_audit_ai.domain.chat.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessageEntity, Long> {

    // 인용을 함께 보여줘야 해서 N+1을 피하기 위해 left join fetch 한다.
    @Query("""
            select distinct message
            from ChatMessageEntity message
            left join fetch message.citations
            where message.conversation.id = :conversationId
            order by message.createdAt asc, message.id asc
            """)
    List<ChatMessageEntity> findAllByConversationIdWithCitations(
            @Param("conversationId") Long conversationId
    );

    long countByConversation_Id(Long conversationId);
}
