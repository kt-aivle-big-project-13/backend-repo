package com.aivle13.fin_audit_ai.global.llm.client;

import com.aivle13.fin_audit_ai.global.exception.llm.LlmServerErrorException;
import com.aivle13.fin_audit_ai.global.exception.llm.LlmServerTimeoutException;
import com.aivle13.fin_audit_ai.global.llm.ReportLlmClient;
import com.aivle13.fin_audit_ai.global.llm.config.LlmProperties;
import com.aivle13.fin_audit_ai.global.llm.dto.OpenAiResponse;
import com.aivle13.fin_audit_ai.global.llm.dto.OpenAiResponseRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Objects;

@Component
public class OpenAiReportLlmClient implements ReportLlmClient {

    private static final String RESPONSES_PATH = "/responses";
    private static final String OUTPUT_TEXT_TYPE = "output_text";
    private static final String COMPLETED_STATUS = "completed";

    private final RestClient openAiRestClient;
    private final LlmProperties properties;

    public OpenAiReportLlmClient(
            @Qualifier("openAiRestClient")
            RestClient openAiRestClient,
            LlmProperties properties
    ) {
        this.openAiRestClient = openAiRestClient;
        this.properties = properties;
    }

    // 시스템·사용자 프롬프트를 OpenAI에 전달해 보고서 본문 생성
    @Override
    public String generate(String systemPrompt, String userPrompt) {
        OpenAiResponseRequest request =
                new OpenAiResponseRequest(
                        properties.model(),
                        systemPrompt,
                        userPrompt,
                        false
                );

        try {
            OpenAiResponse response = openAiRestClient.post()
                    .uri(RESPONSES_PATH)
                    .body(request)
                    .retrieve()
                    .body(OpenAiResponse.class);

            return extractGeneratedText(response);

        } catch (ResourceAccessException exception) {
            if (hasTimeoutCause(exception)) {
                throw new LlmServerTimeoutException(exception);
            }
            throw new LlmServerErrorException(exception);

        } catch (RestClientException exception) {
            throw new LlmServerErrorException(exception);
        }
    }

    // OpenAI 응답에서 비어 있지 않은 output_text만 추출해 보고서 본문으로 반환
    private String extractGeneratedText(OpenAiResponse response) {
        if (response == null || response.output() == null) {
            throw new LlmServerErrorException("OpenAI 응답 본문이 비어 있습니다.");
        }

        if (!COMPLETED_STATUS.equals(response.status())) {
            throw new LlmServerErrorException("OpenAI 응답이 완료되지 않았습니다: " + response.status());
        }

        return response.output().stream()
                .filter(Objects::nonNull)
                .filter(outputItem -> outputItem.content() != null)
                .flatMap(outputItem -> outputItem.content().stream())
                .filter(Objects::nonNull)
                .filter(contentItem -> OUTPUT_TEXT_TYPE.equals(contentItem.type()))
                .map(OpenAiResponse.ContentItem::text)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .reduce(
                        (first, second) ->
                                first
                                        + System.lineSeparator()
                                        + second
                )
                .orElseThrow(() ->
                        new LlmServerErrorException("OpenAI 응답에서 생성된 보고서 본문을 찾을 수 없습니다."));
    }

    // 예외 원인 체인을 확인해 연결 또는 응답 타임아웃 여부 판별
    private boolean hasTimeoutCause(Throwable throwable) {
        Throwable cause = throwable;

        while (cause != null) {
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}