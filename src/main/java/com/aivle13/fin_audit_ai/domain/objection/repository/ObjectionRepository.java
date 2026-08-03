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

    // 상세 조회·대응문서 생성·재생성 시 사용자 소유권 검증
    Optional<ObjectionEntity> findByIdAndModel_User_Id(
            Long id,
            Long userId
    );

    // 발송 시 사용자 소유권 검증과 동시 발송 방지 잠금 적용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select objection
            from ObjectionEntity objection
            where objection.id = :id
              and objection.model.user.id = :userId
            """)
    Optional<ObjectionEntity> findByIdAndModel_User_IdForUpdate(
            @Param("id") Long id,
            @Param("userId") Long userId
    );
}
