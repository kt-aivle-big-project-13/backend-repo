package com.aivle13.fin_audit_ai.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "auditTaskExecutor")
    public Executor auditTaskExecutor() {
        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("audit-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        return executor;
    }

    /**
     * 보고서 선생성 전용 풀.
     *
     * <p>분석용 풀과 나눈 이유는 리포트 생성이 건당 수십 초이기 때문이다(LLM 호출 + PDF 렌더).
     * 같은 풀을 쓰면 리포트가 스레드를 오래 붙잡아 다른 감사의 분석이 밀린다.
     *
     * <p>평상시 동시 실행은 코어 스레드 수인 3개다. 한 감사가 분석 직후 제출하는 3종을 한 번에
     * 만들 수 있는 크기이면서, 그 이상은 큐에 쌓아 AI 서버에 한꺼번에 몰리지 않게 한다.
     *
     * <p>{@code maxPoolSize} 는 평상시에는 쓰이지 않는다. {@code ThreadPoolExecutor} 는 큐가
     * 가득 찬 뒤에야 코어를 넘어 스레드를 늘리므로, 대기가 50건을 넘는 폭주 상황에서만 6개까지
     * 늘어나는 마지막 완충 장치다. 거기까지 넘치면 버린다 — 선생성은 부수 작업이고, 버려져도
     * 사용자가 다운로드할 때 기존 경로로 만들어지기 때문이다.
     */
    @Bean(name = "reportTaskExecutor")
    public Executor reportTaskExecutor() {
        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("report-pregen-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.DiscardPolicy()
        );
        executor.initialize();

        return executor;
    }

    /**
     * SHAP·Fairlearn 병렬 분석 전용 풀.
     *
     * <p>{@code AuditAnalysisEventListener.handle()} 자체가 {@code @Async("auditTaskExecutor")}로
     * 실행되며 두 분석이 모두 끝날 때까지 그 스레드 하나를 붙잡고 대기한다. 두 분석 제출까지
     * 같은 {@code auditTaskExecutor}에 맡기면, 대기 중인 리스너 스레드가 코어 자리를 하나
     * 이미 차지하고 있어(코어가 꽉 차야만 큐 밖으로 스레드가 늘어나는 게 기본 동작) 나머지
     * 분석 두 개가 실제로는 같은 풀 안에서 순차로 밀려 실행된다 — 병렬화 효과가 사라진다.
     * 그래서 분석 제출은 별도 풀에서 돌려 리스너 스레드와 경합하지 않게 한다.
     *
     * <p>코어 4개는 감사 2건이 동시에 분석 중이어도(건당 SHAP·Fairlearn 2개씩) 대기 없이
     * 바로 병렬 실행되는 크기다. 그 이상 몰리면 큐에 쌓였다가 처리된다.
     */
    @Bean(name = "auditAnalysisExecutor")
    public Executor auditAnalysisExecutor() {
        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("audit-analysis-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        return executor;
    }
}