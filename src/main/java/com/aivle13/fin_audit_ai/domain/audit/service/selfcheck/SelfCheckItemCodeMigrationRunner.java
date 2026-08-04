package com.aivle13.fin_audit_ai.domain.audit.service.selfcheck;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 자가점검 문항이 5개(NOTICE 등) → 21개(TR-01 등)로 확장되면서, 기존 5문항과 질문이 완전히
 * 동일한 5개는 새 코드로 흡수됐다: NOTICE→TR_01, OBJECTION→UP_01, OVERSIGHT→HO_01,
 * RISK_MANAGEMENT→RM_01, DOCUMENTATION→DC_01. 과거 저장된 감사 응답이 새 21문항 화면에서도
 * 그대로 보이도록, 이 러너가 딱 한 번 옛 코드를 새 코드로 옮긴다.
 *
 * <p>answer_value 컬럼은 이번에 새로 추가되는 컬럼이라(boolean→3값 확장, 기존 answer 컬럼은
 * ddl-auto:update로 타입을 바꿀 수 없어 새로 만듦) 과거 행엔 기본값('NO')만 채워져 있다.
 * 옛 answer(boolean) 컬럼의 실제 값을 읽어 정확한 값(YES/NO)으로 다시 채운 뒤 코드를 옮긴다.
 *
 * <p>WHERE item_code IN (옛 코드)만 골라 옮기므로, 이미 옮겨진 행은 다시 안 걸려 재기동해도
 * 안전하다 — LawArticleSeeder와 동일한 멱등 패턴.
 */
@Slf4j
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class SelfCheckItemCodeMigrationRunner implements ApplicationRunner {

    private static final Map<String, String> LEGACY_CODE_RENAME = Map.of(
            "NOTICE", "TR_01",
            "OBJECTION", "UP_01",
            "OVERSIGHT", "HO_01",
            "RISK_MANAGEMENT", "RM_01",
            "DOCUMENTATION", "DC_01"
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // Hibernate가 @Enumerated(STRING) 컬럼에 자동으로 만드는 CHECK 제약은 ddl-auto:update가
        // 절대 갱신하지 않는다 — enum이 5개(NOTICE 등)이던 시절에 만들어진 제약이 그대로 남아있어,
        // 새 21개 코드(TR_01 등)로 저장하려 하면 애플리케이션 검증을 통과해도 DB가 거부한다
        // (직접 재현해서 확인함). 제약을 지워야 새 코드가 실제로 저장된다.
        jdbcTemplate.execute(
                "ALTER TABLE self_check_answers DROP CONSTRAINT IF EXISTS self_check_answers_item_code_check"
        );

        // 옛 answer(boolean) 컬럼을 읽어야 하는 아래 UPDATE는, 이 러너가 이미 한 번 실행돼
        // 컬럼을 지운 뒤에는 "column answer does not exist"로 실패한다(devtools 자동 재시작
        // 때 재현해서 확인함). 컬럼이 아직 있을 때만 백필을 시도해 멱등하게 만든다.
        Boolean legacyColumnExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns "
                        + "WHERE table_name = 'self_check_answers' AND column_name = 'answer')",
                Boolean.class
        );

        if (Boolean.TRUE.equals(legacyColumnExists)) {
            int migrated = 0;

            for (Map.Entry<String, String> rename : LEGACY_CODE_RENAME.entrySet()) {
                migrated += jdbcTemplate.update(
                        "UPDATE self_check_answers "
                                + "SET answer_value = CASE WHEN answer THEN 'YES' ELSE 'NO' END, item_code = ? "
                                + "WHERE item_code = ?",
                        rename.getValue(), rename.getKey()
                );
            }

            if (migrated > 0) {
                log.info("자가점검 문항 코드 마이그레이션 완료: {}건 (옛 5문항 코드 → 신규 21문항 코드)", migrated);
            }

            // 옛 answer(boolean) 컬럼은 엔티티에서 이미 안 쓰는데 DB엔 여전히 NOT NULL로
            // 남아있어, 새 코드가 이 컬럼에 값을 안 넣으니 모든 INSERT가 막힌다(직접 재현해서
            // 확인함: "null value in column answer violates not-null constraint"). 위
            // UPDATE에서 이미 다 읽었으니(answer_value로 옮김) 컬럼째로 지운다.
            jdbcTemplate.execute("ALTER TABLE self_check_answers DROP COLUMN IF EXISTS answer");
        }
    }
}
