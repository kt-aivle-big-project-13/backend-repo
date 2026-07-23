package com.aivle13.fin_audit_ai.domain.auth.dto.response;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;

public record TokenResponse(
        String accessToken,     // API 요청에 사용
        String refreshToken,    // Access Token 재발급에 사용
        String tokenType,       // 토큰 인증 방식
        long expiresIn,         // Access Token 만료시간(초)
        Long userId,            // 로그인한 사용자의 DB 식별자
        String email,           // 로그인한 사용자의 이메일
        String name,            // 로그인한 사용자의 이름
        UserRole role           // 로그인한 사용자의 권한
) {

    // 발급된 토큰과 로그인 사용자 정보를 응답 객체로 변환
    public static TokenResponse of(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UserEntity user
    ) {
        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn,
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }
}