package com.aivle13.fin_audit_ai.domain.board.repository;

import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PostSpecifications {

    private PostSpecifications() {
    }

    private static final char LIKE_ESCAPE_CHAR = '\\';

    // 제목 또는 내용에 검색어를 포함하는 공지사항. 검색어가 비어있으면 조건을 걸지 않는다.
    // Spring Data JPA 4.x부터 Specification.where(null)이 예외를 던지므로 항상 non-null을 반환한다.
    public static Specification<PostEntity> keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return (root, query, cb) -> cb.conjunction();
        }

        String like = "%" + escapeLike(keyword.trim()) + "%";

        return (root, query, cb) -> cb.or(
                cb.like(root.get("title"), like, LIKE_ESCAPE_CHAR),
                cb.like(root.get("content"), like, LIKE_ESCAPE_CHAR)
        );
    }

    // LIKE 패턴의 메타문자(%, _)와 이스케이프 문자 자체(\)를 리터럴로 취급하도록 이스케이프한다.
    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    public static Specification<PostEntity> orderByCreatedAt(boolean oldestFirst) {
        return (root, query, cb) -> {
            query.orderBy(
                    oldestFirst ? cb.asc(root.get("createdAt")) : cb.desc(root.get("createdAt")),
                    oldestFirst ? cb.asc(root.get("id")) : cb.desc(root.get("id"))
            );

            return cb.conjunction();
        };
    }
}
