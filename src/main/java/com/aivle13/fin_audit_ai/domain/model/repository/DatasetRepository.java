package com.aivle13.fin_audit_ai.domain.model.repository;

import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<DatasetEntity, Long> {
}
