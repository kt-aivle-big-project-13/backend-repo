package com.aivle13.fin_audit_ai.domain.board.repository;

import com.aivle13.fin_audit_ai.domain.board.entity.PostAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostAttachmentRepository extends JpaRepository<PostAttachmentEntity, Long> {

    List<PostAttachmentEntity> findByPost_Id(Long postId);

    List<PostAttachmentEntity> findByPost_IdAndIdIn(Long postId, List<Long> ids);

    Optional<PostAttachmentEntity> findByIdAndPost_Id(Long id, Long postId);

    long countByPost_Id(Long postId);
}