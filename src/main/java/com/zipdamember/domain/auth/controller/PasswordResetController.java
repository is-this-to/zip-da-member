package com.zipdamember.domain.auth.controller;

import com.zipdamember.domain.auth.request.PasswordResetEmailRequest;
import com.zipdamember.domain.auth.request.PasswordResetRequest;
import com.zipdamember.domain.auth.response.PasswordResetGuideResponse;
import com.zipdamember.domain.auth.service.PasswordResetService;
import com.zipdamember.global.response.GlobalResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    @Operation(summary = "비밀번호 재설정 링크 요청", description = "계정 존재 여부와 무관하게 같은 응답을 반환합니다.")
    @SecurityRequirements
    @PreAuthorize("!isAuthenticated()")
    @PostMapping("/api/member/auth/password-reset-requests")
    public ResponseEntity<GlobalResponseDTO<PasswordResetGuideResponse>> requestPasswordReset(
            @Valid @RequestBody PasswordResetEmailRequest request
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                passwordResetService.requestReset(request)
        ));
    }

    @Operation(summary = "비밀번호 재설정")
    @SecurityRequirements
    @PreAuthorize("!isAuthenticated()")
    @PostMapping("/api/member/auth/password-resets")
    public ResponseEntity<GlobalResponseDTO<Void>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(GlobalResponseDTO.success());
    }
}
