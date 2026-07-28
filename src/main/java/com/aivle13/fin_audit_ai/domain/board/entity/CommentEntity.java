package com.aivle13.fin_audit_ai.domain.board.entity;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자만 작성할 수 있는 게시글 답변(댓글). 작성 권한은 SecurityConfig에서 ROLE_ADMIN으로 제한한다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_comments")
public class CommentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private UserEntity author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public static CommentEntity create(PostEntity post, UserEntity author, String content) {
        CommentEntity comment = new CommentEntity();
        comment.post = post;
        comment.author = author;
        comment.content = content;
        return comment;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}