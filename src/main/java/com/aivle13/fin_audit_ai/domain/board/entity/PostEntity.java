package com.aivle13.fin_audit_ai.domain.board.entity;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_posts")
public class PostEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private UserEntity author;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 관리자가 지정하는 공지 여부. 목록에서 항상 최상단에 고정 노출된다.
    @Column(nullable = false)
    private boolean pinned = false;

    public static PostEntity create(UserEntity author, String title, String content) {
        PostEntity post = new PostEntity();
        post.author = author;
        post.title = title;
        post.content = content;
        post.pinned = false;
        return post;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void updatePinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isAuthor(Long userId) {
        return this.author.getId().equals(userId);
    }
}