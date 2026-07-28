package com.aivle13.fin_audit_ai.domain.board.repository;

import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PostSpecifications {

    private PostSpecifications() {
    }

    // 제목 또는 내용에 검색어를 포함하는 게시글. 검색어가 비어있으면 조건을 걸지 않는다.
    public static Specification<PostEntity> keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }

        String like = "%" + keyword.trim() + "%";

        return (root, query, cb) -> cb.or(
                cb.like(root.get("title"), like),
                cb.like(root.get("content"), like)
        );
    }
}