package com.zipdamember.domain.auth.controller;

import com.zipdamember.domain.auth.request.CreateMemberRequest;
import com.zipdamember.domain.auth.request.LoginRequest;
import com.zipdamember.domain.auth.request.SocialSignupRequest;
import com.zipdamember.domain.auth.request.SocialAccountLinkRequest;
import com.zipdamember.domain.auth.response.CreateMemberResponse;
import com.zipdamember.domain.auth.response.LoginResponse;
import com.zipdamember.domain.auth.response.MemberPrincipalResponse;
import com.zipdamember.domain.auth.response.SocialSignupContextResponse;
import com.zipdamember.domain.auth.service.AuthService;
import com.zipdamember.domain.auth.service.SocialAuthService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증/인가 약관 API")
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final SocialAuthService socialAuthService;

    @Operation(summary = "회원 회원가입", description = "USER 회원가입")
    @CustomApiResponse(value = {
            CustomResponseCode.INVALID_PARAMETER_ERROR,

            CustomResponseCode.NOT_FOUND_ERROR,

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
    @PostMapping("/api/member")
    public ResponseEntity<GlobalResponseDTO<CreateMemberResponse>> signup(@Valid @RequestBody CreateMemberRequest request) {
        CreateMemberResponse response = authService.signup(request);

        return ResponseEntity.ok(GlobalResponseDTO.success(response));
    }

    @Operation(summary = "카카오 회원가입 정보 조회", description = "카카오 인증 후 추가 정보 입력 화면에 이메일·닉네임·프로필을 제공합니다.")
    @SecurityRequirements
    @PreAuthorize("!isAuthenticated()")
    @GetMapping("/api/member/auth/social-signups/current")
    public ResponseEntity<GlobalResponseDTO<SocialSignupContextResponse>> getSocialSignupContext(
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                socialAuthService.getSignupContext(request)
        ));
    }

    @Operation(summary = "카카오 소셜 회원가입", description = "카카오 인증 정보와 추가 입력값으로 회원·소셜 계정·약관 이력을 저장합니다.")
    @SecurityRequirements
    @PreAuthorize("!isAuthenticated()")
    @PostMapping("/api/member/auth/social-signups")
    public ResponseEntity<GlobalResponseDTO<CreateMemberResponse>> socialSignup(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse,
            @Valid @RequestBody SocialSignupRequest request
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                socialAuthService.signup(servletRequest, servletResponse, request)
        ));
    }

    @Operation(summary = "카카오 계정 연결", description = "같은 이메일의 일반 회원이 비밀번호 재인증과 명시적 동의 후 카카오 계정을 연결합니다.")
    @SecurityRequirements
    @PreAuthorize("!isAuthenticated()")
    @PostMapping("/api/member/auth/social-links")
    public ResponseEntity<GlobalResponseDTO<Void>> linkSocialAccount(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse,
            @Valid @RequestBody SocialAccountLinkRequest request
    ) {
        socialAuthService.linkLocalAccount(servletRequest, servletResponse, request);
        return ResponseEntity.ok(GlobalResponseDTO.success());
    }

    @Operation(summary = "회원 이메일 로그인", description = "USER·AGENT 공통 로그인")
    @SecurityRequirements
    @PostMapping("/api/member/auth/sessions")
    public ResponseEntity<GlobalResponseDTO<LoginResponse<MemberPrincipalResponse>>> login(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(authService.login(request, response, loginRequest)));
    }

    @Operation(summary = "회원 토큰 재발급", description = "회원 Refresh 쿠키로 토큰 회전")
    @SecurityRequirements
    @PostMapping("/api/member/auth/token-refreshes")
    public ResponseEntity<GlobalResponseDTO<LoginResponse<MemberPrincipalResponse>>> reissue(
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
