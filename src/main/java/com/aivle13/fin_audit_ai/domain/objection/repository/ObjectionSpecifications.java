package com.aivle13.fin_audit_ai.domain.objection.repository;

import com.aivle13.fin_audit_ai.domain.objection.entity.ObjectionEntity;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.List;

public final class ObjectionSpecifications {

    private static final char LIKE_ESCAPE_CHAR = '\\';

    private ObjectionSpecifications() {
    }

    // 고객명 또는 제목에 검색어를 포함하는 이의제기. 검색어가 비어있으면 조건을 걸지 않는다.
    public static Specification<ObjectionEntity> keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return (root, query, cb) -> cb.conjunction();
        }

        String like = "%" + escapeLike(keyword.trim()) + "%";

        return (root, query, cb) -> cb.or(
                cb.like(root.get("customerName"), like, LIKE_ESCAPE_CHAR),
                cb.like(root.get("title"), like, LIKE_ESCAPE_CHAR)
        );
    }

    public static Specification<ObjectionEntity> statusIn(List<ObjectionStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<ObjectionEntity> orderBySubmittedAt(boolean oldestFirst) {
        return (root, query, cb) -> {
            query.orderBy(oldestFirst
                    ? cb.asc(root.get("submittedAt"))
                    : cb.desc(root.get("submittedAt")));
            return cb.conjunction();
        };
    }

    public static Specification<ObjectionEntity> ownedBy(
            Long userId
    ) {
        return (root, query, cb) -> cb.equal(
                root.get("model")
                        .get("user")
                        .get("id"),
                userId
        );
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}