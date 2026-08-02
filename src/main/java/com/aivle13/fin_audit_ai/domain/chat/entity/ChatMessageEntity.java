package com.aivle13.fin_audit_ai.domain.chat.entity;

import com.aivle13.fin_audit_ai.domain.chat.type.ChatRole;
import com.aivle13.fin_audit_ai.domain.chat.type.GroundingStatus;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** 대화 안의 메시지 한 건. 사용자 질문과 답변을 같은 표에 순서대로 쌓는다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_messages")
public class ChatMessageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id")
    private ChatConversationEntity conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 사용자 질문에는 근거 판정이 없다.
    @Enumerated(EnumType.STRING)
    @Column(name = "grounding_status", length = 20)
    private GroundingStatus groundingStatus;

    @OneToMany(
            mappedBy = "message",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ChatMessageCitationEntity> citations = new ArrayList<>();

    public static ChatMessageEntity question(
            ChatConversationEntity conversation,
            String content
    ) {
        ChatMessageEntity message = new ChatMessageEntity();
        message.conversation = conversation;
        message.role = ChatRole.USER;
        message.content = content;

        return message;
    }

    public static ChatMessageEntity answer(
            ChatConversationEntity conversation,
            String content,
            GroundingStatus groundingStatus
    ) {
        ChatMessageEntity message = new ChatMessageEntity();
        message.conversation = conversation;
        message.role = ChatRole.ASSISTANT;
        message.content = content;
        message.groundingStatus = groundingStatus;

        return message;
    }

    public void addCitation(ChatMessageCitationEntity citation) {
        citations.add(citation);
        citation.assignTo(this);
    }
}
