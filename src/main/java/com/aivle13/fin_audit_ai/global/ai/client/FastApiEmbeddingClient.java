package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.global.ai.dto.EmbeddingRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.EmbeddingResponse;
import com.aivle13.fin_audit_ai.global.exception.ai.AiServerErrorException;
import com.aivle13.fin_audit_ai.global.exception.ai.AiServerTimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

/**
 * AI 레포에는 아직 임베딩 생성 엔드포인트가 없어(2026-07-28 기준), SHAP·fairness와 동일한
 * 내부 연동 경로 컨벤션({@code /internal/v1/{feature}/...})으로 이 경로를 잠정 사용한다.
 * AI 서버 실제 배포 경로가 확정되면 경로 상수만 조정하면 된다.
 * RestClient는 SHAP·fairness와 동일하게 shapRestClient를 재사용한다 — AI 서버가 한 대뿐이라
 * base-url/timeout 설정을 기능별로 나눌 이유가 없다.
 */
@Component
public class FastApiEmbeddingClient implements EmbeddingClient {

    private static final String EMBEDDING_GENERATE_PATH =
            "/internal/v1/embedding/generate";

    private final RestClient restClient;

    public FastApiEmbeddingClient(
            @Qualifier("shapRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    @Override
    public float[] embed(String text) {
        try {
            EmbeddingResponse response = restClient.post()
                    .uri(EMBEDDING_GENERATE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new EmbeddingRequest(text))
                    .retrieve()
                    .body(EmbeddingResponse.class);

            if (response == null
                    || response.embedding() == null
                    || response.embedding().length == 0) {
                throw new AiServerErrorException();
            }

            return response.embedding();
        } catch (ResourceAccessException exception) {
            if (hasTimeoutCause(exception)) {
                throw new AiServerTimeoutException();
            }

            throw new AiServerErrorException(exception);
        } catch (RestClientException exception) {
            throw new AiServerErrorException(exception);
        }
    }

    private boolean hasTimeoutCause(Throwable throwable) {
        Throwable cause = throwable;

        while (cause != null) {
            if (cause instanceof HttpTimeoutException
                    || cause instanceof SocketTimeoutException) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}
