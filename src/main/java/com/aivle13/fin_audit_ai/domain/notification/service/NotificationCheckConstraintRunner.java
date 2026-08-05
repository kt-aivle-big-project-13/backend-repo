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

        log.info("notifications 테이블 ck_notifications_type_reference 제약을 현재 코드 기준으로 동기화했습니다.");
    }
}
