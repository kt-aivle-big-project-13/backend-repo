package com.aivle13.fin_audit_ai.domain.board.repository;

import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

public final class PostSpecifications {

    private PostSpecifications() {
    }

    private static final char LIKE_ESCAPE_CHAR = '\\';

    // 제목 또는 내용에 검색어를 포함하는 게시글. 검색어가 비어있으면 조건을 걸지 않는다.
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

    // 공지(pinned)는 정렬 옵션과 무관하게 항상 최신순으로 최상단에 노출하고,
    // 일반 게시글만 요청한 정렬(최신순/오래된순)을 따른다.
    // 한 컬럼에는 방향을 하나만 줄 수 있어, 그룹별로 값이 없는 쪽은 NULL이 되는
    // CASE 식 두 개를 만들어 각각 원하는 방향으로 정렬한다. 같은 그룹 안에서는
    // 서로 반대쪽 CASE 값이 전부 NULL(동률)이라 실제 정렬에 영향을 주지 않는다.
    // Pageable에 Sort를 함께 넘기면 Spring Data가 이 orderBy를 덮어써버리므로,
    // 호출 측에서는 반드시 정렬 없는 Pageable과 함께 사용해야 한다.
    public static Specification<PostEntity> orderByPinnedFirst(boolean oldestFirst) {
        return (root, query, cb) -> {
            var pinnedCreatedAt = cb.<LocalDateTime>selectCase()
                    .when(cb.isTrue(root.get("pinned")), root.<LocalDateTime>get("createdAt"))
                    .otherwise(cb.nullLiteral(LocalDateTime.class));

            var unpinnedCreatedAt = cb.<LocalDateTime>selectCase()
                    .when(cb.isFalse(root.get("pinned")), root.<LocalDateTime>get("createdAt"))
                    .otherwise(cb.nullLiteral(LocalDateTime.class));

            // createdAt이 같은 게시글이 있으면 DB가 순서를 보장하지 않아 페이지 분할 시
            // 중복/누락이 생길 수 있다. id를 마지막 결정적 tie-breaker로 추가하되, pinned는
            // 정렬 옵션과 무관하게 항상 최신순이어야 하므로 createdAt과 동일하게 그룹별로 분리한다.
            var pinnedId = cb.<Long>selectCase()
                    .when(cb.isTrue(root.get("pinned")), root.<Long>get("id"))
                    .otherwise(cb.nullLiteral(Long.class));

            var unpinnedId = cb.<Long>selectCase()
                    .when(cb.isFalse(root.get("pinned")), root.<Long>get("id"))
                    .otherwise(cb.nullLiteral(Long.class));

            query.orderBy(
                    cb.desc(root.get("pinned")),
                    cb.desc(pinnedCreatedAt),
                    cb.desc(pinnedId),
                    oldestFirst ? cb.asc(unpinnedCreatedAt) : cb.desc(unpinnedCreatedAt),
                    oldestFirst ? cb.asc(unpinnedId) : cb.desc(unpinnedId)
            );

            return cb.conjunction();
        };
    }
}