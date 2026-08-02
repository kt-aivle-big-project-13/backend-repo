package com.aivle13.fin_audit_ai.domain.chat.entity;

import com.aivle13.fin_audit_ai.domain.chat.type.CitationType;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 답변이 인용한 근거.
 *
 * <p>답변 본문과 분리해 저장하는 이유는 규제 대응 증적이다. "그때 그 답변의 근거가
 * 무엇이었나"를 나중에 재구성할 수 있어야 하고, 법령이 개정되면 과거 답변이 어떤
 * 조항에 기반했는지 추적할 수 있어야 한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_message_citations")
public class ChatMessageCitationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "citation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id")
    private ChatMessageEntity message;

    @Enumerated(EnumType.STRING)
    @Column(name = "citation_type", nullable = false, length = 20)
    private CitationType citationType;

    @Column(nullable = false, length = 255)
    private String reference;

    @Column(length = 255)
    private String snippet;

    @Column(precision = 6, scale = 4)
    private BigDecimal similarity;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    public static ChatMessageCitationEntity of(
            CitationType citationType,
            String reference,
            String snippet,
            BigDecimal similarity,
            String sourceUrl
    ) {
        ChatMessageCitationEntity citation = new ChatMessageCitationEntity();
        citation.citationType = citationType;
        citation.reference = reference;
        citation.snippet = snippet;
        citation.similarity = similarity;
        citation.sourceUrl = sourceUrl;

        return citation;
    }

    void assignTo(ChatMessageEntity message) {
        this.message = message;
    }
}
