package com.aivle13.fin_audit_ai.domain.board.entity;

import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_post_attachments")
public class PostAttachmentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private PostEntity post;

    // prefix + '/' + UUID + '_' + 원본 파일명으로 구성돼 원본 파일명(255자)보다
    // 길어질 수 있으므로 여유 있게 잡는다.
    @Column(name = "file_key", nullable = false, length = 512)
    private String fileKey;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(nullable = false)
    private long size;

    public static PostAttachmentEntity create(PostEntity post, StoredFile stored) {
        PostAttachmentEntity attachment = new PostAttachmentEntity();
        attachment.post = post;
        attachment.fileKey = stored.s3Key();
        attachment.originalName = stored.originalName();
        attachment.contentType = stored.contentType();
        attachment.size = stored.size();
        return attachment;
    }
}