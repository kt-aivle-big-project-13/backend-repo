package com.aivle13.fin_audit_ai.domain.diagnosis.repository;

import com.aivle13.fin_audit_ai.domain.diagnosis.entity.PreDiagnosisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreDiagnosisRepository extends JpaRepository<PreDiagnosisEntity, Long> {
}
