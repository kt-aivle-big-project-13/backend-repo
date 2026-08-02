package com.aivle13.fin_audit_ai.domain.chat.entity;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    // 대화를 지우면 그 안의 메시지도 남을 이유가 없다. 매핑이 없으면 대화만 지워져
    // chat_messages.conversation_id 외래키 제약에 걸리므로, 여기서 함께 지운다.
    // 메시지에 딸린 인용은 ChatMessageEntity 의 cascade 로 이어서 지워진다.
    @OneToMany(
            mappedBy = "conversation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ChatMessageEntity> messages = new ArrayList<>();

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
