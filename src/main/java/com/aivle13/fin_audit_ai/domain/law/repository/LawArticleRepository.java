package com.aivle13.fin_audit_ai.domain.law.repository;

import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LawArticleRepository extends JpaRepository<LawArticleEntity, Long> {

    boolean existsByLawNameAndArticleNo(String lawName, String articleNo);

    List<LawArticleEntity> findByEmbeddingIsNull();
}
