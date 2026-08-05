package com.aivle13.fin_audit_ai.global.ai.client.report;

import com.aivle13.fin_audit_ai.global.ai.dto.report.request.ImprovementGuideRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.ImprovementGuideResponse;
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
public class FastApiImprovementGuideClient
        implements ImprovementGuideClient {

    private static final String IMPROVEMENT_GUIDE_PATH =
            "/internal/v1/reports/improvement";

    private final RestClient restClient;

    public FastApiImprovementGuideClient(
            @Qualifier("reportRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    @Override
    public ImprovementGuideResponse generate(
            ImprovementGuideRequest request
    ) {
        try {
            ImprovementGuideResponse response = restClient.post()
                    .uri(IMPROVEMENT_GUIDE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ImprovementGuideResponse.class);

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
