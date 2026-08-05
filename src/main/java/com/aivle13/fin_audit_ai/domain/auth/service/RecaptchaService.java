package com.aivle13.fin_audit_ai.domain.auth.service;

import com.aivle13.fin_audit_ai.domain.auth.dto.response.RecaptchaVerifyResponse;
import com.aivle13.fin_audit_ai.global.exception.external.RecaptchaServerException;
import com.aivle13.fin_audit_ai.global.exception.user.auth.RecaptchaVerificationFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Service
public class RecaptchaService {

    private final RestClient restClient;
    private final String secretKey;
    private final String verifyUrl;

    public RecaptchaService(
            @Value("${recaptcha.secret-key}") String secretKey,
            @Value("${recaptcha.verify-url}") String verifyUrl,
            @Value("${recaptcha.connect-timeout}") Duration connectTimeout,
            @Value("${recaptcha.read-timeout}") Duration readTimeout
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        this.secretKey = secretKey;
        this.verifyUrl = verifyUrl;
    }

    public void verify(String recaptchaToken) {
        MultiValueMap<String, String> requestBody =
                new LinkedMultiValueMap<>();

        requestBody.add("secret", secretKey);
        requestBody.add("response", recaptchaToken);

        try {
            RecaptchaVerifyResponse response = restClient
                    .post()
                    .uri(verifyUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(requestBody)
                    .retrieve()
                    .body(RecaptchaVerifyResponse.class);

            if (response == null || !response.success()) {
                throw new RecaptchaVerificationFailedException();
            }
        } catch (RestClientException exception) {
            throw new RecaptchaServerException();
        }
    }
}