package com.aivle13.fin_audit_ai.domain.demo.controller;

import com.aivle13.fin_audit_ai.domain.auth.dto.response.TokenResponse;
import com.aivle13.fin_audit_ai.domain.demo.service.DemoAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Demo",
        description = "시연용 게스트 접속 API"
)
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class DemoAuthController {

    private final DemoAccountService demoAccountService;

    @Operation(
            summary = "테스트 로그인",
            description = """
                    회원가입 없이 둘러볼 수 있는 게스트 계정을 발급합니다. 응답은 일반 로그인과
                    같은 토큰 형식이며, 계정에는 데모 모델·데이터셋과 완료된 감사 결과가 함께
                    만들어집니다. `app.demo.enabled` 가 켜진 환경에서만 동작합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "게스트 계정 발급 성공"),
            @ApiResponse(responseCode = "404", description = "시연 모드가 꺼져 있음")
    })
    @PostMapping("/demo")
    public ResponseEntity<TokenResponse> issueGuest() {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(demoAccountService.issueGuest());
    }
}
