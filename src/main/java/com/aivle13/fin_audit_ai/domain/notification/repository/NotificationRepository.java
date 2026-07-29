package com.aivle13.fin_audit_ai.domain.notification.repository;

import com.aivle13.fin_audit_ai.domain.notification.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Page<NotificationEntity> findByUser_IdOrderBySentAtDesc(Long userId, Pageable pageable);

    Optional<NotificationEntity> findByIdAndUser_Id(Long id, Long userId);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.user.id = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    // 알림 벨 "모두 읽음" 처리. 알림 건수가 많아질 수 있어 엔티티를 각각 로드하지 않고 벌크 업데이트한다.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);
}