package com.aivle13.fin_audit_ai.domain.objection.repository;

import com.aivle13.fin_audit_ai.domain.objection.entity.ObjectionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ObjectionRepository
        extends JpaRepository<ObjectionEntity, Long>, JpaSpecificationExecutor<ObjectionEntity> {

    boolean existsByObjectionNo(String objectionNo);

    // 같은 이의제기에 대한 동시 발송 요청이 상태 체크를 동시에 통과해 메일이 중복 발송되지 않도록
    // 행 단위로 잠가 요청을 직렬화한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ObjectionEntity o where o.id = :id")
    Optional<ObjectionEntity> findByIdForUpdate(@Param("id") Long id);
}