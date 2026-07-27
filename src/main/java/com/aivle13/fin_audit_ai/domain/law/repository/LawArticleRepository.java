package com.aivle13.fin_audit_ai.domain.law.repository;

import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LawArticleRepository extends JpaRepository<LawArticleEntity, Long> {

    boolean existsByLawNameAndArticleNo(String lawName, String articleNo);
}
