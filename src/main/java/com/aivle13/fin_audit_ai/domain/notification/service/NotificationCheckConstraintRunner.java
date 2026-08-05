package com.aivle13.fin_audit_ai.domain.notification.service;

import com.aivle13.fin_audit_ai.domain.notification.entity.NotificationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * NotifType에 값이 늘어날 때마다 NotificationEntity의 ck_notifications_type_reference
 * CHECK 제약 본문도 같이 바뀌지만, ddl-auto:update는 이미 존재하는 제약은 이름이 같으면
 * 본문이 달라져도 절대 갱신하지 않는다(SelfCheckItemCodeMigrationRunner와 동일한 한계,
 * 직접 재현해서 확인함). 매 기동마다 제약을 지웠다 코드의 현재 상수로 다시 만들어
 * 로컬 DB를 코드와 동기화한다 — DROP/ADD 모두 IF EXISTS라 몇 번을 재기동해도 안전하다.
 *
 * <p>notifications_notif_type_check는 Hibernate가 notif_type 컬럼(@Enumerated STRING)에
 * 자동으로 만든 별개의 CHECK 제약이라, 마찬가지로 ddl-auto:update가 새 NotifType 값을
 * 반영 안 해준다(직접 재현해서 확인함: AUDIT_FAILED insert가 이 제약에 막혀 markFailed
 * 트랜잭션 전체가 롤백되고 감사가 PENDING에 멈춰버림). 위 ck_notifications_type_reference가
 * notif_type 값 자체도 이미 열거해서 검증하므로, 이건 재생성하지 않고 지우기만 한다
 * (self_check_answers_item_code_check를 지우기만 했던 것과 동일한 패턴).
 */
@Slf4j
@Component
@Profile("dev")
@Order(3)
@RequiredArgsConstructor
public class NotificationCheckConstraintRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute(
                "ALTER TABLE notifications DROP CONSTRAINT IF EXISTS "
                        + NotificationEntity.TYPE_REFERENCE_CONSTRAINT_NAME
        );

        jdbcTemplate.execute(
                "ALTER TABLE notifications ADD CONSTRAINT "
                        + NotificationEntity.TYPE_REFERENCE_CONSTRAINT_NAME
                        + " CHECK (" + NotificationEntity.TYPE_REFERENCE_CONSTRAINT_BODY + ")"
        );

        jdbcTemplate.execute(
                "ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_notif_type_check"
        );

        log.info("notifications 테이블 CHECK 제약을 현재 코드 기준으로 동기화했습니다.");
    }
}
