package com.aivle13.fin_audit_ai.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        int status,
        LocalDateTime timestamp,
        String path,
        List<FieldError> errors
) {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(),
                errorCode.getHttpStatus().value(), now(), path, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String customMessage, String path) {
        return new ErrorResponse(errorCode.getCode(), customMessage,
                errorCode.getHttpStatus().value(), now(), path, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors, String path) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(),
                errorCode.getHttpStatus().value(), now(), path, errors);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FieldError(String field, String value, String reason) {

        public static FieldError of(org.springframework.validation.FieldError error) {
            Object rejectedValue = error.getRejectedValue();   // 한 번만 호출
            return new FieldError(
                    error.getField(),
                    rejectedValue == null ? "" : rejectedValue.toString(),
                    error.getDefaultMessage()
            );
        }
    }
}