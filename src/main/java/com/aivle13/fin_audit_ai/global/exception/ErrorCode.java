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
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "EC008", "지원하지 않는 미디어 타입입니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "EC009", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "EC999", "서버 오류가 발생했습니다."),

    // ===== User / Auth (EU) =====
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "EU001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "EU002", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "EU003", "이메일 또는 비밀번호가 일치하지 않습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "EU004", "인증번호가 일치하지 않거나 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "EU005", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EU006", "만료된 토큰입니다. 재로그인이 필요합니다."),
    INVALID_RESET_TOKEN(HttpStatus.UNAUTHORIZED, "EU007", "재설정 토큰이 만료되었거나 유효하지 않습니다."),
    PASSWORD_RESET_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "EU008", "일치하는 회원 정보가 없습니다."),
    INVALID_PASSWORD_POLICY(HttpStatus.BAD_REQUEST, "EU009", "영문, 숫자, 특수문자( ( ) < > \\\" ' ; 제외 ) 중 2종류를 조합하여 10~16자리, 3종류는 8~16자리로 입력해주세요."),
    PASSWORD_CONFIRM_NOT_MATCH(HttpStatus.BAD_REQUEST, "EU010","비밀번호가 일치하지 않습니다."),

    // ===== Model / Audit (EM) =====
    MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "EM001", "모델을 찾을 수 없습니다."),
    INVALID_MODEL_FILE(HttpStatus.BAD_REQUEST, "EM002", "지원하지 않는 모델 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "EM003", "파일 크기가 허용 범위를 초과했습니다."),
    AUDIT_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "EM004", "이미 감사가 진행 중입니다."),
    AUDIT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EM005", "감사 처리 중 오류가 발생했습니다."),
    INVALID_THRESHOLD_POLICY(HttpStatus.BAD_REQUEST, "EM006", "임계값 설정이 올바르지 않습니다."),
    AUDIT_NOT_FOUND(HttpStatus.NOT_FOUND, "EM007", "감사를 찾을 수 없습니다."),
    EXPLAINABILITY_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "EM008", "설명가능성 감사 결과를 찾을 수 없습니다."),
    AUDIT_NOT_COMPLETED(HttpStatus.CONFLICT, "EM009", "아직 완료되지 않은 감사입니다."),
    INVALID_SENSITIVE_ATTRIBUTE(HttpStatus.BAD_REQUEST, "EM010", "데이터셋에 존재하지 않는 컬럼입니다."),
    DATASET_NOT_FOUND(HttpStatus.NOT_FOUND, "EM011", "데이터셋을 찾을 수 없습니다."),
    SENSITIVE_ATTRIBUTES_NOT_SELECTED(HttpStatus.BAD_REQUEST, "EM012", "민감정보가 선택되지 않았습니다."),
    DATASET_ALREADY_AUDITED(HttpStatus.CONFLICT, "EM013", "이미 감사에 사용된 데이터셋은 민감정보를 수정할 수 없습니다."),
    FAIRNESS_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "EM014", "공정성 감사 결과를 찾을 수 없습니다."),

    // ===== External API 연동 (EA) =====
    AI_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "EA001", "AI 서버 요청에 실패했습니다."),
    AI_SERVER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "EA002", "AI 서버 응답 시간이 초과되었습니다."),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "EA003", "외부 서비스 요청에 실패했습니다."),
    EMAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "EA004", "이메일 발송에 실패했습니다."),

    // ===== File Storage (EF) =====
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EF001", "파일 업로드에 실패했습니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "EF002", "업로드할 파일이 존재하지 않습니다."),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "EF003", "지원하지 않는 파일 형식입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}