package com.zipdamember.domain.auth.controller;

import com.zipdamember.domain.auth.request.LoginRequest;
import com.zipdamember.domain.auth.response.LoginResponse;
import com.zipdamember.domain.auth.service.AuthService;
import com.zipdamember.global.response.GlobalResponseDTO;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증/인가 약관 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/auth")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "회원 이메일 로그인", description = "USER·AGENT 공통 로그인")
    @SecurityRequirements
    @PostMapping("/sessions")
    public ResponseEntity<GlobalResponseDTO<LoginResponse>> login(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(authService.login(request, response, loginRequest)));
    }

    @Operation(summary = "회원 토큰 재발급", description = "회원 Refresh 쿠키로 토큰 회전")
    @SecurityRequirements
    @PostMapping("/token-refreshes")
    public ResponseEntity<GlobalResponseDTO<LoginResponse>> reissue(
        HttpServletRequest request, HttpServletResponse response
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(authService.reissue(request, response)));
    }

    @Operation(summary = "회원 현재 세션 로그아웃")
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @DeleteMapping("/sessions/current")
    public ResponseEntity<GlobalResponseDTO<Void>> logout(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication
    ) {
        authService.logout(request, response, Long.parseLong(authentication.getName()));
        return ResponseEntity.ok(GlobalResponseDTO.success());
    }
}
