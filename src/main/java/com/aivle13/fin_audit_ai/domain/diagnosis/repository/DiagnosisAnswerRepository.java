package com.aivle13.fin_audit_ai.domain.diagnosis.repository;

import com.aivle13.fin_audit_ai.domain.diagnosis.entity.DiagnosisAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiagnosisAnswerRepository
        extends JpaRepository<DiagnosisAnswerEntity, Long> {

    List<DiagnosisAnswerEntity>
    findAllByDiagnosis_IdOrderByIdAsc(
            Long assessmentId
    );
}
