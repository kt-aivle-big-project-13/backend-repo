package com.aivle13.fin_audit_ai.domain.chat.entity;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 감사 결과에 대한 질의 대화.
 *
 * <p>대화는 감사에 종속된다. 감사 없이는 의미가 없으므로 별도 보존 정책을 두지 않고,
 * 감사가 지워지면 함께 지워진다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_conversations")
public class ChatConversationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id")
    private AuditEntity audit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false, length = 100)
    private String title;

    public static ChatConversationEntity create(
            AuditEntity audit,
            UserEntity user,
            String title
    ) {
        ChatConversationEntity conversation = new ChatConversationEntity();
        conversation.audit = audit;
        conversation.user = user;
        conversation.title = title;

        return conversation;
    }
}
