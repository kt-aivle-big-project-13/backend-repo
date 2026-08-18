package com.aivle13.fin_audit_ai.domain.report.service.common;

import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportStatus;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 같은 리포트를 두 번 만들지 않게 한다.
 *
 * <p>리포트 한 건이 LLM 호출·figure 생성·PDF 렌더를 포함해 수십 초가 걸린다. 선생성과
 * 사용자 요청이 겹치거나 다운로드를 연타하면 그만큼이 통째로 중복되고, S3 객체와
 * {@code reports} 행도 여러 벌 쌓인다.
 *
 * <p>이미 만들어진 것이 있으면 그것을 돌려주고, 만드는 중이면 끝날 때까지 기다렸다가 그
 * 결과를 쓴다. 기다리는 쪽도 어차피 생성 시간을 기다릴 참이었으므로 체감은 같고, AI 서버
 * 호출만 한 번으로 줄어든다.
 *
 * <p>잠금은 Redis 에 둔다. 인스턴스가 여러 대라 JVM 안의 잠금으로는 서로 다른 인스턴스로
 * 들어온 요청을 막지 못한다.
 *
 * <p>잠금은 어디까지나 중복을 줄이기 위한 것이라, Redis 가 응답하지 않으면 잠금 없이
 * 진행한다. 중복 생성이 나는 편이 리포트를 아예 못 만드는 것보다 낫다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportGenerationGuard {

    private static final String KEY_PREFIX = "report:generating:";

    // 잠금을 쥔 인스턴스가 죽어도 영원히 남지 않게 하는 안전장치.
    // 생성이 100초 안팎이라 그보다 넉넉히 잡는다.
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ReportRepository reportRepository;

    // 잠금을 못 잡으면 이만큼까지 기다린다. 넘기면 잠금 없이 진행한다.
    @Value("${app.report.generation-lock-max-wait-seconds:600}")
    private long maxWaitSeconds;

    @Value("${app.report.generation-lock-poll-interval-millis:2000}")
    private long pollIntervalMillis;

    /**
     * 이미 있으면 그 리포트를, 없으면 새로 만든 리포트를 돌려준다.
     *
     * @param primaryFormat 이 리포트 종류의 대표 포맷. 종류마다 다르다 —
     *                      고영향 사전진단만 HTML 없이 PDF 가 대표다.
     */
    public Long generateOnce(
            Long userId,
            Long auditId,
            ReportType reportType,
            ReportFormat primaryFormat,
            Supplier<Long> generator
    ) {
        return generateOnce(
                auditId,
                reportType,
                () -> Optional.ofNullable(
                        findCompletedReportId(userId, auditId, reportType, primaryFormat)
                ),
                generator
        );
    }

    /**
     * 포맷이 여럿이라 결과가 id 하나로 떨어지지 않는 리포트에 쓴다.
     *
     * @param findExisting 이미 만들어진 결과. 비어 있으면 새로 만든다.
     */
    public <T> T generateOnce(
            Long auditId,
            ReportType reportType,
            Supplier<Optional<T>> findExisting,
            Supplier<T> generator
    ) {
        Optional<T> existing = findExisting.get();

        if (existing.isPresent()) {
            log.info(
                    "이미 생성된 리포트를 재사용합니다: auditId={}, reportType={}",
                    auditId, reportType
            );

            return existing.get();
        }

        String key = key(auditId, reportType);
        boolean locked = acquire(key);

        try {
            // 잠금을 기다리는 동안 다른 요청이 생성을 끝냈을 수 있다.
            Optional<T> generatedByOther = findExisting.get();

            if (generatedByOther.isPresent()) {
                log.info(
                        "다른 요청이 생성한 리포트를 재사용합니다: auditId={}, reportType={}",
                        auditId, reportType
                );

                return generatedByOther.get();
            }

            return generator.get();
        } finally {
            if (locked) {
                release(key);
            }
        }
    }

    /**
     * 완료된 리포트의 id. 없으면 null.
     *
     * <p>소유자까지 조건에 넣어, 남의 감사 리포트를 재사용해 돌려주지 않게 한다.
     */
    public Long findCompletedReportId(
            Long userId,
            Long auditId,
            ReportType reportType,
            ReportFormat primaryFormat
    ) {
        return reportRepository
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
                        auditId, userId, reportType, primaryFormat
                )
                .filter(report -> report.getStatus() == ReportStatus.COMPLETED)
                .map(ReportEntity::getId)
                .orElse(null);
    }

    /** 잠금을 잡을 때까지 기다린다. 잡았으면 true, 기다리다 포기했으면 false. */
    private boolean acquire(String key) {
        long deadline = System.nanoTime()
                + Duration.ofSeconds(maxWaitSeconds).toNanos();

        while (true) {
            if (setIfAbsent(key)) {
                return true;
            }

            if (System.nanoTime() >= deadline) {
                log.warn("리포트 생성 잠금을 기다리다 포기하고 그대로 진행합니다: key={}", key);

                return false;
            }

            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                return false;
            }
        }
    }

    private boolean setIfAbsent(String key) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL)
            );
        } catch (RuntimeException exception) {
            // Redis 가 죽어도 리포트 생성 자체는 되어야 한다. 잠금을 잡은 것으로 치고
            // 진행하되, 해제 시도도 같은 이유로 조용히 넘어간다.
            log.warn("리포트 생성 잠금을 확인하지 못해 잠금 없이 진행합니다: key={}", key, exception);

            return true;
        }
    }

    private void release(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException exception) {
            log.warn("리포트 생성 잠금을 해제하지 못했습니다. TTL 로 풀립니다: key={}", key, exception);
        }
    }

    private String key(Long auditId, ReportType reportType) {
        return "%s%d:%s".formatted(KEY_PREFIX, auditId, reportType.name());
    }
}
