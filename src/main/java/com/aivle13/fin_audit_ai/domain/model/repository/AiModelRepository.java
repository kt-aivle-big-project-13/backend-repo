package com.aivle13.fin_audit_ai.domain.model.repository;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiModelRepository extends JpaRepository<AiModelEntity, Long> {

    // 감사 시작 시 "모델당 진행 중인 감사 1건" 검증과 생성을 원자적으로 만들기 위해
    // 모델 row에 락을 걸어 같은 모델에 대한 동시 요청을 직렬화한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from AiModelEntity m where m.id = :id")
    Optional<AiModelEntity> findByIdForUpdate(@Param("id") Long id);
}
