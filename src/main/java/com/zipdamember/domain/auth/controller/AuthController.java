package com.zipdamember.domain.auth.controller;

import com.zipdamember.domain.auth.request.CreateMemberRequest;
import com.zipdamember.domain.auth.request.LoginRequest;
import com.zipdamember.domain.auth.response.CreateMemberResponse;
import com.zipdamember.domain.auth.response.LoginResponse;
import com.zipdamember.domain.auth.service.AuthService;
import com.zipdamember.global.config.openapi.CustomApiResponse;
import com.zipdamember.global.response.GlobalResponseDTO;
import com.zipdamember.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증/인가 약관 API")
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "회원 회원가입", description = "USER 회원가입")
    @CustomApiResponse(value = {
            CustomResponseCode.INVALID_PARAMETER_ERROR,

            CustomResponseCode.ALREADY_REGISTERED_ERROR,
            CustomResponseCode.DUPLICATED_RESOURCE_ERROR,

            CustomResponseCode.EMAIL_VERIFICATION_NOT_FOUND,
            CustomResponseCode.EMAIL_VERIFICATION_EMAIL_MISMATCH,
            CustomResponseCode.EMAIL_VERIFICATION_EXPIRED,
            CustomResponseCode.EMAIL_VERIFICATION_REQUIRED,

            CustomResponseCode.UNAUTHORIZED_ERROR,

            CustomResponseCode.DB_ERROR,
            CustomResponseCode.DB_DUPLICATED_KEY_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("!isAuthenticated()")
    @PostMapping("/api/members")
    public ResponseEntity<GlobalResponseDTO<CreateMemberResponse>> signup(@Valid @RequestBody CreateMemberRequest request) {
        CreateMemberResponse response = authService.signup(request);

        return ResponseEntity.ok(GlobalResponseDTO.success(response));
    }

    @Operation(summary = "회원 이메일 로그인", description = "USER·AGENT 공통 로그인")
    @SecurityRequirements
    @PostMapping("/api/member/auth/sessions")
    public ResponseEntity<GlobalResponseDTO<LoginResponse>> login(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(authService.login(request, response, loginRequest)));
    }

    @Operation(summary = "회원 토큰 재발급", description = "회원 Refresh 쿠키로 토큰 회전")
    @SecurityRequirements
    @PostMapping("/api/member/auth/token-refreshes")
    public ResponseEntity<GlobalResponseDTO<LoginResponse>> reissue(
        HttpServletRequest request, HttpServletResponse response
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(authService.reissue(request, response)));
    }

    @Operation(summary = "회원 현재 세션 로그아웃")
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @DeleteMapping("/api/member/auth/sessions/current")
    public ResponseEntity<GlobalResponseDTO<Void>> logout(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication
    ) {
        authService.logout(request, response, Long.parseLong(authentication.getName()));
        return ResponseEntity.ok(GlobalResponseDTO.success());
    }
}
