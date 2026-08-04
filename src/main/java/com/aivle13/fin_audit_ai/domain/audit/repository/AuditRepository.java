package com.aivle13.fin_audit_ai.domain.audit.repository;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.projection.ReportPreGenerationTargetProjection;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AuditRepository extends JpaRepository<AuditEntity, Long> {

    Optional<AuditEntity> findByIdAndUser_Id(Long auditId, Long userId);

    // 보고서 선생성은 소유자 id 와 사전진단 연결 여부만 필요하다. 엔티티를 읽으면 지연로딩된
    // 사용자를 트랜잭션 밖에서 건드리게 되므로 스칼라 두 개만 가져온다.
    @Query("""
            select audit.user.id as userId,
                   audit.assessmentId as assessmentId
            from AuditEntity audit
            where audit.id = :auditId
            """)
    Optional<ReportPreGenerationTargetProjection> findReportPreGenerationTargetById(
            @Param("auditId") Long auditId
    );

    // 동일 감사에 대한 동시 쓰기(예: 자율점검 응답 동시 저장)를 직렬화하기 위한 비관적 쓰기 잠금 조회.
    // 잠금은 호출한 트랜잭션이 끝날 때까지 유지된다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select audit from AuditEntity audit where audit.id = :auditId and audit.user.id = :userId")
    Optional<AuditEntity> findByIdAndUser_IdForUpdate(
            @Param("auditId") Long auditId,
            @Param("userId") Long userId
    );

    // 자율점검 저장은 사용자 요청마다 매핑 이벤트를 발행하므로, 동일 auditId에 대한
    // 매핑 재생성(AuditRegulationMappingService)이 겹치지 않도록 같은 방식으로 직렬화한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select audit from AuditEntity audit where audit.id = :auditId")
    Optional<AuditEntity> findByIdForUpdate(@Param("auditId") Long auditId);

    boolean existsByModel_IdAndStatusIn(Long modelId, List<AuditStatus> statuses);

    // 감사 취소 시, 같은 모델에 이번 건 말고도(IdNot) 취소 아닌(StatusNot) 감사가 남아있는지
    // 확인한다. 없으면 이 모델은 실사용 이력이 없는 것이므로 AiModelEntity.archive() 대상이 된다.
    boolean existsByModel_IdAndIdNotAndStatusNot(Long modelId, Long excludeAuditId, AuditStatus status);

    List<AuditEntity> findAllByStatus(AuditStatus status);

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

    // 리포트 생성은 트랜잭션 밖에서 validationDataset을 읽으므로(open-in-view=false)
    // 여기서 함께 가져온다. nullable이라 left join fetch를 쓴다.
    @Query("""
            select audit
            from AuditEntity audit
            join fetch audit.model
            join fetch audit.dataset
            left join fetch audit.validationDataset
            where audit.id = :auditId
              and audit.user.id = :userId
            """)
    Optional<AuditEntity> findByIdAndUser_IdWithModelAndDataset(
            @Param("auditId") Long auditId,
            @Param("userId") Long userId
    );

    @Query("""
        select audit
        from AuditEntity audit
        where audit.model.id = :modelId
          and audit.status in :statuses
        order by
          case when audit.completedAt is null then 1 else 0 end,
          audit.completedAt desc,
          audit.id desc
        """)
    List<AuditEntity> findLatestByModelIdAndStatuses(
            @Param("modelId") Long modelId,
            @Param("statuses") List<AuditStatus> statuses,
            Pageable pageable
    );
}
