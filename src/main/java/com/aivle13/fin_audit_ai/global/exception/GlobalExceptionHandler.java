package com.aivle13.fin_audit_ai.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        log.error("[BusinessException] Error Code: {}, Message: {}", errorCode.getCode(), errorCode.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .message(e.getMessage())
                .build();

        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {

        log.error("[Unhandled Exception] Message: {}", e.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .errorCode("E999")
                .message("알 수 없는 에러가 발생했습니다.")
                .build();

        return new ResponseEntity<>(response, ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus());
    }
}
