package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.global.ai.dto.ExplainabilityReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.ExplainabilityReportResponse;
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
public class FastApiExplainabilityReportClient
        implements ExplainabilityReportClient {

    private static final String EXPLAINABILITY_REPORT_PATH =
            "/internal/v1/reports/explainability";

    private final RestClient restClient;

    public FastApiExplainabilityReportClient(
            @Qualifier("reportRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    @Override
    public ExplainabilityReportResponse generate(
            ExplainabilityReportRequest request
    ) {
        try {
            ExplainabilityReportResponse response = restClient.post()
                    .uri(EXPLAINABILITY_REPORT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ExplainabilityReportResponse.class);

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
