package com.aivle13.fin_audit_ai.domain.board.repository;

import com.aivle13.fin_audit_ai.domain.board.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    // 같은 트랜잭션에서 연속 저장된 댓글은 createdAt이 동일할 수 있어 id를 보조 정렬 키로 둔다.
    List<CommentEntity> findByPost_IdOrderByCreatedAtAscIdAsc(Long postId);

    List<CommentEntity> findByPost_Id(Long postId);

    Optional<CommentEntity> findByIdAndPost_Id(Long id, Long postId);

    // 목록 화면에서 게시글별 댓글 수를 한 번의 쿼리로 집계한다(N+1 방지).
    @Query("""
            SELECT c.post.id AS postId, COUNT(c) AS commentCount
            FROM CommentEntity c
            WHERE c.post.id IN :postIds
            GROUP BY c.post.id
            """)
    List<PostCommentCountProjection> countByPostIds(@Param("postIds") List<Long> postIds);
}