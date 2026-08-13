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
     * <p>평상시 동시 실행은 코어 스레드 수인 1개다. 한 감사가 제출하는 리포트들을 한꺼번에
     * 보내지 않고 한 건씩 순차로 만든다.
     *
     * <p>코어를 3에서 1로 줄인 이유는 받는 쪽이 감당하지 못했기 때문이다. AI 서버는 uvicorn
     * 워커 1개에 vCPU 2개인데, 리포트 요청 하나가 LLM 호출·figure 생성·Chromium 실행을 모두
     * 포함한다. 감사 2건이 겹쳤을 때 리포트 6건이 분석 요청과 함께 물려 AI 서버 프로세스가
     * 메모리 고갈로 강제 종료됐고, 진행 중이던 SHAP 분석이 사라져 감사가 실패로 끝났다.
     *
     * <p>선생성은 부수 작업이라 늦어지거나 버려져도 기능에 영향이 없다. 조회·다운로드 경로
     * ({@code BiasReportQueryService} 등)는 저장된 행만 읽고 없으면 404 를 주지만, 클라이언트가
     * 다운로드 전에 생성 엔드포인트({@code POST /audits/{auditId}/reports/*})를 먼저 호출한다
     * — 조회가 404 면 생성부터 하고 다시 조회한다. 그래서 선생성이 버려진 리포트도 사용자에게는
     * 404 가 아니라 생성 시간만큼의 대기로 나타난다. 반면 밀려나는 쪽은 본 작업인 분석이므로,
     * 둘이 경합하면 선생성이 양보하는 것이 맞다.
     *
     * <p>{@code queueCapacity} 도 함께 줄인다. 같은 큐 길이라도 코어가 3에서 1로 줄면 대기가
     * 해소되는 데 걸리는 시간은 3배가 된다(50건 / 동시 3건 ≈ 17 사이클 → 50 사이클). 리포트
     * 한 건이 수십 초라 50 사이클이면 30분이 넘는 대기가 쌓이고, 그동안 밀린 선생성 요청이
     * 새로 시작한 감사의 분석과 계속 경합한다. 대기 해소 시간을 이전과 비슷하게 유지하도록
     * 20으로 맞춘다.
     *
     * <p>{@code maxPoolSize} 는 평상시에는 쓰이지 않는다. {@code ThreadPoolExecutor} 는 큐가
     * 가득 찬 뒤에야 코어를 넘어 스레드를 늘리므로, 대기가 20건을 넘는 폭주 상황에서만 2개까지
     * 늘어나는 마지막 완충 장치다. 즉 한꺼번에 몰리면 실행 2건 + 대기 20건까지 22건을 받고
     * 나머지는 {@code DiscardPolicy} 로 버린다 — 위와 같은 이유로 버려져도 되기 때문이다.
     */
    @Bean(name = "reportTaskExecutor")
    public Executor reportTaskExecutor() {
        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
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