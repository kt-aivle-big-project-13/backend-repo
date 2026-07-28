package com.aivle13.fin_audit_ai.domain.board.repository;

import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PostSpecifications {

    private PostSpecifications() {
    }

    // 제목 또는 내용에 검색어를 포함하는 게시글. 검색어가 비어있으면 조건을 걸지 않는다.
    // Spring Data JPA 4.x부터 Specification.where(null)이 예외를 던지므로 항상 non-null을 반환한다.
    public static Specification<PostEntity> keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return (root, query, cb) -> cb.conjunction();
        }

        String like = "%" + keyword.trim() + "%";

        return (root, query, cb) -> cb.or(
                cb.like(root.get("title"), like),
                cb.like(root.get("content"), like)
        );
    }
}