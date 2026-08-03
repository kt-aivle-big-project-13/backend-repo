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
}