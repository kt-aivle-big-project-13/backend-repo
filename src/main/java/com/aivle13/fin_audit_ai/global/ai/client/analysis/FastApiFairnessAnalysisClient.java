package com.aivle13.fin_audit_ai.global.ai.client.analysis;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.fairness.FairnessRunRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness.FairnessRunResponse;
import com.aivle13.fin_audit_ai.global.ai.dto.analysis.request.FairnessAnalysisRequest;
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
import java.util.Arrays;
import java.util.List;

/**
 * AI 팀 원본 계약 문서상 경로는 {@code POST /api/fairness/audits}이나(FairnessRunResponse 참고),
 * 이 프로젝트에서 SHAP 연동에 쓰는 내부 연동 경로 컨벤션({@code /internal/v1/{feature}/analyze})과
 * 일관되도록 이 경로를 잠정 사용한다. AI 서버 실제 배포 경로가 확정되면 조정이 필요하다.
 */
@Component
public class FastApiFairnessAnalysisClient implements FairnessAnalysisClient {

    private static final String FAIRNESS_ANALYSIS_PATH =
            "/internal/v1/fairness/analyze";

    private final RestClient restClient;

    public FastApiFairnessAnalysisClient(
            @Qualifier("shapRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    @Override
    public FairnessRunResponse analyze(FairnessRunRequest request) {
        try {
            FairnessRunResponse response = restClient.post()
                    .uri(FAIRNESS_ANALYSIS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toAiRequest(request))
                    .retrieve()
                    .body(FairnessRunResponse.class);

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

    private FairnessAnalysisRequest toAiRequest(FairnessRunRequest request) {
        return new FairnessAnalysisRequest(
                request.auditId(),
                request.modelFileKey(),
                request.auditDatasetFileKey(),
                request.validationDatasetFileKey(),
                request.auditName(),
                request.targetApprovalRate(),
                request.manualThreshold(),
                parseSensitiveFeatures(request.sensitiveFeatures())
        );
    }

    private List<String> parseSensitiveFeatures(String sensitiveFeatures) {
        return Arrays.stream(sensitiveFeatures.split(","))
                .map(String::trim)
                .filter(feature -> !feature.isBlank())
                .distinct()
                .toList();
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
