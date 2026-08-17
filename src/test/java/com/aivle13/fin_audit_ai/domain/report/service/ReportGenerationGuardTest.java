package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportRepository;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportGenerationGuard;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportStatus;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 리포트 중복 생성 방지 단위 테스트.
 *
 * <p>같은 리포트를 두 번 만들지 않는지, 그리고 잠금이 어떤 경우에도 남지 않는지를 본다.
 */
@ExtendWith(MockitoExtension.class)
class ReportGenerationGuardTest {

    private static final Long USER_ID = 2L;
    private static final Long AUDIT_ID = 21L;
    private static final Long EXISTING_REPORT_ID = 31L;
    private static final Long NEW_REPORT_ID = 41L;
    private static final String LOCK_KEY = "report:generating:21:BIAS_REPORT";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ReportRepository reportRepository;

    private ReportGenerationGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ReportGenerationGuard(redisTemplate, reportRepository);

        // 잠금 대기를 짧게 줄여 폴링 경로도 테스트에서 돌 수 있게 한다.
        ReflectionTestUtils.setField(guard, "maxWaitSeconds", 2L);
        ReflectionTestUtils.setField(guard, "pollIntervalMillis", 10L);
    }

    @Test
    @DisplayName("이미 만들어진 리포트가 있으면 생성하지 않고 그 id 를 돌려준다")
    void reusesAlreadyGeneratedReport() {
        givenExistingReport(completedReport());

        AtomicInteger generatorCalls = new AtomicInteger();

        Long reportId = guard.generateOnce(
                USER_ID, AUDIT_ID, ReportType.BIAS_REPORT, ReportFormat.HTML,
                () -> {
                    generatorCalls.incrementAndGet();
                    return NEW_REPORT_ID;
                }
        );

        assertThat(reportId).isEqualTo(EXISTING_REPORT_ID);
        assertThat(generatorCalls.get()).isZero();
        // 재사용 경로에서는 잠금을 건드리지 않는다.
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("만들어진 리포트가 없으면 생성한다")
    void generatesWhenAbsent() {
        givenNoExistingReport();
        givenLockAcquired();

        Long reportId = guard.generateOnce(
                USER_ID, AUDIT_ID, ReportType.BIAS_REPORT, ReportFormat.HTML,
                () -> NEW_REPORT_ID
        );

        assertThat(reportId).isEqualTo(NEW_REPORT_ID);
    }

    @Test
    @DisplayName("잠금을 잡은 뒤 다른 요청이 만들어 둔 리포트를 찾으면 그것을 쓴다")
    void reusesReportGeneratedWhileWaiting() {
        // 스텁 안에서 또 스텁하면 Mockito 가 끊긴 스텁으로 보므로 먼저 만들어 둔다.
        ReportEntity generatedByOther = completedReport();

        given(reportRepository
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
                        anyLong(), anyLong(), any(), any()))
                .willReturn(Optional.empty(), Optional.of(generatedByOther));
        givenLockAcquired();

        AtomicInteger generatorCalls = new AtomicInteger();

        Long reportId = guard.generateOnce(
                USER_ID, AUDIT_ID, ReportType.BIAS_REPORT, ReportFormat.HTML,
                () -> {
                    generatorCalls.incrementAndGet();
                    return NEW_REPORT_ID;
                }
        );

        assertThat(reportId).isEqualTo(EXISTING_REPORT_ID);
        assertThat(generatorCalls.get()).isZero();
    }

    @Test
    @DisplayName("생성이 끝나면 잠금을 해제한다")
    void releasesLockAfterGeneration() {
        givenNoExistingReport();
        givenLockAcquired();

        guard.generateOnce(
                USER_ID, AUDIT_ID, ReportType.BIAS_REPORT, ReportFormat.HTML,
                () -> NEW_REPORT_ID
        );

        verify(redisTemplate).delete(LOCK_KEY);
    }

    @Test
    @DisplayName("생성이 실패해도 잠금을 해제한다")
    void releasesLockWhenGenerationFails() {
        givenNoExistingReport();
        givenLockAcquired();

        assertThatThrownBy(() -> guard.generateOnce(
                USER_ID, AUDIT_ID, ReportType.BIAS_REPORT, ReportFormat.HTML,
                () -> {
                    throw new IllegalStateException("AI 서버 실패");
                }
        )).isInstanceOf(IllegalStateException.class);

        verify(redisTemplate).delete(LOCK_KEY);
    }

    @Test
    @DisplayName("잠금을 끝내 못 잡으면 기다리다 포기하고 그대로 생성한다")
    void generatesAnywayWhenLockNeverFrees() {
        givenNoExistingReport();
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class)))
                .willReturn(false);

        Long reportId = guard.generateOnce(
                USER_ID, AUDIT_ID, ReportType.BIAS_REPORT, ReportFormat.HTML,
                () -> NEW_REPORT_ID
        );

        assertThat(reportId).isEqualTo(NEW_REPORT_ID);
        // 잡은 적이 없으므로 해제도 하지 않는다.
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("Redis 가 응답하지 않아도 리포트는 생성한다")
    void generatesWhenRedisUnavailable() {
        givenNoExistingReport();
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willThrow(new RedisConnectionFailureException("redis down"));

        Long reportId = guard.generateOnce(
                USER_ID, AUDIT_ID, ReportType.BIAS_REPORT, ReportFormat.HTML,
                () -> NEW_REPORT_ID
        );

        assertThat(reportId).isEqualTo(NEW_REPORT_ID);
    }

    @Test
    @DisplayName("완료되지 않은 리포트는 재사용하지 않는다")
    void doesNotReuseUnfinishedReport() {
        ReportEntity generating = mock(ReportEntity.class);
        given(generating.getStatus()).willReturn(ReportStatus.GENERATING);
        givenExistingReport(generating);
        givenLockAcquired();

        Long reportId = guard.generateOnce(
                USER_ID, AUDIT_ID, ReportType.BIAS_REPORT, ReportFormat.HTML,
                () -> NEW_REPORT_ID
        );

        assertThat(reportId).isEqualTo(NEW_REPORT_ID);
    }

    private ReportEntity completedReport() {
        ReportEntity report = mock(ReportEntity.class);
        given(report.getStatus()).willReturn(ReportStatus.COMPLETED);
        given(report.getId()).willReturn(EXISTING_REPORT_ID);

        return report;
    }

    private void givenExistingReport(ReportEntity report) {
        given(reportRepository
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
                        anyLong(), anyLong(), any(), any()))
                .willReturn(Optional.of(report));
    }

    private void givenNoExistingReport() {
        given(reportRepository
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
                        anyLong(), anyLong(), any(), any()))
                .willReturn(Optional.empty());
    }

    private void givenLockAcquired() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class)))
                .willReturn(true);
    }
}
