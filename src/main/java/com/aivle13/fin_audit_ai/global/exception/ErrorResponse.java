package com.aivle13.fin_audit_ai.global.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private String errorCode;
    private String message;

}