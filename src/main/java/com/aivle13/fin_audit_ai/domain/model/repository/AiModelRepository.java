package com.aivle13.fin_audit_ai.domain.model.repository;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiModelRepository extends JpaRepository<AiModelEntity, Long> {

    // 소유자가 아닌 모델은 조회 자체가 안 되게 해서, 존재 여부도 노출하지 않고
    // ModelNotFoundException(404)으로 일관되게 응답한다.
    Optional<AiModelEntity> findByIdAndUser_Id(Long id, Long userId);

    // createdAt이 동률인 경우까지 결정적으로 정렬되도록 id를 보조 정렬 기준으로 둔다.
    List<AiModelEntity> findByUser_IdOrderByCreatedAtDescIdDesc(Long userId);

    // 신규 모델 등록 시 같은 사용자가 이미 쓰고 있는 모델명인지 확인한다.
    boolean existsByUser_IdAndModelName(Long userId, String modelName);

    // (userId, modelName) 조합에 대한 PostgreSQL 트랜잭션 범위 advisory lock을 건다.
    // 같은 이름으로 동시에 두 건의 신규 모델 등록 요청이 들어와도, 뒤에 도착한 트랜잭션은
    // 앞선 트랜잭션이 커밋(=락 해제)할 때까지 여기서 대기하게 되어 existsByUser_IdAndModelName
    // 검증과 save가 사실상 원자적으로 수행된다. 트랜잭션 종료 시 자동 해제되므로 별도 unlock이 필요 없다.
    @Query(value = "SELECT pg_advisory_xact_lock(:lockKey)", nativeQuery = true)
    void lockForModelNameRegistration(@Param("lockKey") long lockKey);

    // 감사 시작 시 "모델당 진행 중인 감사 1건" 검증과 생성을 원자적으로 만들기 위해
    // 모델 row에 락을 걸어 같은 모델에 대한 동시 요청을 직렬화한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from AiModelEntity m where m.id = :id and m.user.id = :userId")
    Optional<AiModelEntity> findByIdAndUser_IdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}