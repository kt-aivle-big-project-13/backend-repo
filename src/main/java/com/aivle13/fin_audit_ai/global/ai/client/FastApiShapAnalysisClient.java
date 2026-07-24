package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.explainability.ExplainabilityResultRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.ShapAnalysisRequest;
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

@Component
public class FastApiShapAnalysisClient implements ShapAnalysisClient {

    private static final String SHAP_ANALYSIS_PATH =
            "/internal/v1/shap/analyze";

    private final RestClient restClient;

    public FastApiShapAnalysisClient(
            @Qualifier("shapRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    @Override
    public ExplainabilityResultRequest analyze(
            ShapAnalysisRequest request
    ) {
        try {
            ExplainabilityResultRequest response = restClient.post()
                    .uri(SHAP_ANALYSIS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ExplainabilityResultRequest.class);

            if (response == null) {
                throw new AiServerErrorException();
            }

            return response;
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