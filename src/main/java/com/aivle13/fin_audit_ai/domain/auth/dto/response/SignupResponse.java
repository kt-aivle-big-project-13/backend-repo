package com.aivle13.fin_audit_ai.domain.auth.dto.response;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;

public record SignupResponse(
        Long id,
        String email,
        String name
) {

    public static SignupResponse from(UserEntity user) {
        return new SignupResponse(user.getId(), user.getEmail(), user.getName());
    }
}