package com.aivle13.fin_audit_ai.global.migration;

import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 마이그레이션이 테스트에서 실제로 적용되는지 확인한다.
 *
 * <p>테스트가 {@code ddl-auto} 로 스키마를 만들면 마이그레이션 SQL 은 배포 시점에 처음
 * 실행된다. 실제로 #319 에서 같은 버전의 마이그레이션이 둘 있는데도 CI 가 전부 통과하고
 * prod 배포에서야 드러났다. 그 구성으로 되돌아가면 이 테스트가 먼저 깨진다.
 *
 * <p>버전 중복 자체는 Flyway 가 기동에서 막아 준다. 다만 그러려면 테스트에서 Flyway 가
 * 돌고 있어야 하므로, 그 전제를 여기서 지킨다.
 */
class FlywayMigrationTest extends IntegrationTestSupport {

    private static final String MIGRATION_LOCATION = "classpath:db/migration/V*.sql";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("마이그레이션이 실제로 적용된다")
    void migrationsAreApplied() {
        List<String> appliedVersions = appliedVersions();

        assertThat(appliedVersions)
                .as("flyway_schema_history 가 비어 있으면 테스트가 ddl-auto 로 스키마를 만든 것이다")
                .isNotEmpty();
    }

    @Test
    @DisplayName("적용된 마이그레이션에 실패한 것이 없다")
    void noFailedMigration() {
        Integer failed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false",
                Integer.class
        );

        assertThat(failed).isZero();
    }

    @Test
    @DisplayName("마이그레이션 파일과 적용된 버전이 일치한다")
    void everyMigrationFileIsApplied() throws IOException {
        assertThat(appliedVersions())
                .as("파일을 추가하고 적용되지 않았거나, 버전 번호가 겹치면 어긋난다")
                .containsExactlyInAnyOrderElementsOf(migrationFileVersions());
    }

    @Test
    @DisplayName("마이그레이션 버전에 중복이 없다")
    void versionsAreUnique() throws IOException {
        List<String> fileVersions = migrationFileVersions();

        assertThat(fileVersions).doesNotHaveDuplicates();
    }

    private List<String> appliedVersions() {
        return jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE version IS NOT NULL",
                String.class
        );
    }

    /** V1__initial_schema.sql -> "1" */
    private List<String> migrationFileVersions() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources(MIGRATION_LOCATION);

        return Arrays.stream(resources)
                .map(Resource::getFilename)
                .filter(Objects::nonNull)
                .map(fileName -> fileName.substring(1, fileName.indexOf("__")))
                .toList();
    }
}
