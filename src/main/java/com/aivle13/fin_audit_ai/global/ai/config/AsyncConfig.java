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
     * <p>한 감사에서 최대 3종을 동시에 만들 수 있게 하되, AI 서버에 몰리지 않도록 상한을 둔다.
     * 큐가 차면 호출한 스레드에서 실행하지 않고 버린다 — 선생성은 부수 작업이고, 실패해도
     * 다운로드 시점에 기존 경로로 만들어지기 때문이다.
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