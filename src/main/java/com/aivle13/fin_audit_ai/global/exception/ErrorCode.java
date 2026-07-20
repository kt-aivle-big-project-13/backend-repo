package com.aivle13.fin_audit_ai.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ===== Common (EC) =====
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "EC001", "입력값이 올바르지 않습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "EC002", "입력 타입이 올바르지 않습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "EC003", "필수 파라미터가 누락되었습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "EC004", "지원하지 않는 HTTP 메서드입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "EC005", "요청한 리소스를 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "EC006", "접근 권한이 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "EC007", "인증이 필요합니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "EC999", "서버 오류가 발생했습니다."),

    // ===== User (EU) =====
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "EU001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "EU002", "이미 사용 중인 이메일입니다."),

    // ===== Model / Audit (EM) =====
    MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "EM001", "모델을 찾을 수 없습니다."),
    INVALID_MODEL_FILE(HttpStatus.BAD_REQUEST, "EM002", "지원하지 않는 모델 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "EM003", "파일 크기가 허용 범위를 초과했습니다."),
    AUDIT_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "EM004", "이미 감사가 진행 중입니다."),
    AUDIT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EM005", "감사 처리 중 오류가 발생했습니다."),

    // ===== AI Server 연동 (EA) =====
    AI_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "EA001", "AI 서버 요청에 실패했습니다."),
    AI_SERVER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "EA002", "AI 서버 응답 시간이 초과되었습니다."),

    // ===== File Storage (EF) =====
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EF001", "파일 업로드에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}