package com.aivle13.fin_audit_ai.domain.user.controller;

import com.aivle13.fin_audit_ai.domain.user.dto.request.PasswordFindRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.PasswordResetRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.response.PasswordFindResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.PasswordResetResponse;
import com.aivle13.fin_audit_ai.domain.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password")
public class UserController {

    private final UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
    }

    @PostMapping("/find")
    public ResponseEntity<PasswordFindResponse>
    findPassword(
            @Valid @RequestBody
            PasswordFindRequest request
    ) {
        PasswordFindResponse response =
                userService.findPassword(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset")
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        PasswordResetResponse response =
                userService.resetPassword(request);

        return ResponseEntity.ok(response);
    }
}