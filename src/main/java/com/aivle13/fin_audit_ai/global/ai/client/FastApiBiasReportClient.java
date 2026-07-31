package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.global.ai.dto.BiasReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.BiasReportResponse;
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
public class FastApiBiasReportClient
        implements BiasReportClient {

    private static final String BIAS_REPORT_PATH =
            "/internal/v1/reports/bias";

    private final RestClient restClient;

    public FastApiBiasReportClient(
            @Qualifier("reportRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    @Override
    public BiasReportResponse generate(
            BiasReportRequest request
    ) {
        try {
            BiasReportResponse response = restClient.post()
                    .uri(BIAS_REPORT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(BiasReportResponse.class);

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
