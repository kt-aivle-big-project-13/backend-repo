package com.aivle13.fin_audit_ai.domain.diagnosis.repository;

import com.aivle13.fin_audit_ai.domain.diagnosis.entity.DiagnosisAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisAnswerRepository extends JpaRepository<DiagnosisAnswerEntity, Long> {
}
