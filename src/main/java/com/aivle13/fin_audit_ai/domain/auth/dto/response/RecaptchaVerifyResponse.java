package com.aivle13.fin_audit_ai.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecaptchaVerifyResponse(

        boolean success,

        @JsonProperty("challenge_ts")
        String challengeTs,

        String hostname,

        @JsonProperty("error-codes")
        List<String> errorCodes

) {
}