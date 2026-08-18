package com.aivle13.fin_audit_ai.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 전역 예외 처리 단위 테스트.
 *
 * <p>DB 제약 위반만 본다. 사전 검사를 통과하고도 동시 요청 때문에 제약에 걸리는 경우가
 * 있어, 그때 500 이 아니라 409 가 나가는지 확인한다.
 */
class GlobalExceptionHandlerTest {

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String NOT_NULL_VIOLATION = "23502";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("중복 키 위반은 409 로 응답한다")
    void duplicateKeyReturnsConflict() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement",
                new SQLException(
                        "duplicate key value violates unique constraint \"uk52txfhvaec38b7jj0o1n4b9cm\"",
                        UNIQUE_VIOLATION
                )
        );

        ResponseEntity<ErrorResponse> response =
                handler.handleDataIntegrityViolation(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.DUPLICATE_RESOURCE.getCode());
    }

    @Test
    @DisplayName("중복 키 위반 응답에 제약명을 노출하지 않는다")
    void duplicateKeyDoesNotLeakConstraintName() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement",
                new SQLException(
                        "duplicate key value violates unique constraint \"uk52txfhvaec38b7jj0o1n4b9cm\"",
                        UNIQUE_VIOLATION
                )
        );

        ResponseEntity<ErrorResponse> response =
                handler.handleDataIntegrityViolation(exception, request());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo(ErrorCode.DUPLICATE_RESOURCE.getMessage())
                .doesNotContain("uk52txfhvaec38b7jj0o1n4b9cm")
                .doesNotContain("constraint");
    }

    @Test
    @DisplayName("중복이 아닌 제약 위반은 500 을 유지한다")
    void otherConstraintViolationStaysServerError() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement",
                new SQLException("null value in column \"title\"", NOT_NULL_VIOLATION)
        );

        ResponseEntity<ErrorResponse> response =
                handler.handleDataIntegrityViolation(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    @DisplayName("SQLException 이 예외 체인 깊숙이 있어도 찾아낸다")
    void findsSqlExceptionDeepInCauseChain() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement",
                new IllegalStateException(
                        "wrapped",
                        new SQLException("duplicate key value", UNIQUE_VIOLATION)
                )
        );

        ResponseEntity<ErrorResponse> response =
                handler.handleDataIntegrityViolation(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("SQLException 이 없으면 500 으로 처리한다")
    void missingSqlExceptionStaysServerError() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("제약 위반이지만 원인을 알 수 없음");

        ResponseEntity<ErrorResponse> response =
                handler.handleDataIntegrityViolation(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/models/1/objections/upload");
        return request;
    }
}
