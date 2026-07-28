package com.aivle13.fin_audit_ai.domain.audit.repository;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuditRepository extends JpaRepository<AuditEntity, Long> {

    Optional<AuditEntity> findByIdAndUser_Id(Long auditId, Long userId);

    // 동일 감사에 대한 동시 쓰기(예: 자율점검 응답 동시 저장)를 직렬화하기 위한 비관적 쓰기 잠금 조회.
    // 잠금은 호출한 트랜잭션이 끝날 때까지 유지된다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select audit from AuditEntity audit where audit.id = :auditId and audit.user.id = :userId")
    Optional<AuditEntity> findByIdAndUser_IdForUpdate(
            @Param("auditId") Long auditId,
            @Param("userId") Long userId
    );

    boolean existsByModel_IdAndStatusIn(Long modelId, List<AuditStatus> statuses);

    // 목록에서 모델명을 같이 보여줘야 해서 N+1을 피하기 위해 model을 fetch join 한다.
    @Query("""
            select audit
            from AuditEntity audit
            join fetch audit.model
            where audit.user.id = :userId
            order by audit.createdAt desc, audit.id desc
            """)
    List<AuditEntity> findByUser_IdOrderByCreatedAtDescIdDesc(@Param("userId") Long userId);

    // validationDataset은 nullable이라 inner join fetch를 쓰면 값이 없는 감사가
    // 통째로 빠지므로 left join fetch로 가져온다.
    @Query("""
            select audit
            from AuditEntity audit
            join fetch audit.model
            join fetch audit.dataset
            left join fetch audit.validationDataset
            where audit.id = :auditId
            """)
    Optional<AuditEntity> findByIdWithModelAndDataset(
            @Param("auditId") Long auditId
    );
}
