package com.zipdamember.domain.auth.controller;

import com.zipdamember.domain.admin.request.AdminLoginRequest;
import com.zipdamember.domain.admin.response.AdminAuthResponse;
import com.zipdamember.domain.auth.service.AdminAuthService;
import com.zipdamember.global.openapi.CustomApiResponse;
import com.zipdamember.global.response.GlobalResponseDTO;
import com.zipdamember.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 인증 API", description = "관리자 인증 담당")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "관리자 로그인 처리", description = "아이디와 비밀번호로 로그인")
    @SecurityRequirements
    @CustomApiResponse(value = {
        CustomResponseCode.NOT_REGISTERED_ERROR,
        CustomResponseCode.INVALID_PARAMETER_ERROR,
        CustomResponseCode.DB_ERROR,
        CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping("/admin-sessions")
    public ResponseEntity<GlobalResponseDTO<AdminAuthResponse>> login(
            @Valid @RequestBody AdminLoginRequest adminLoginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AdminAuthResponse loginResponse = adminAuthService.login(request, response, adminLoginRequest);
        return ResponseEntity.ok(GlobalResponseDTO.success(loginResponse));
    }

    @Operation(summary = "관리자 로그아웃 처리")
    @SecurityRequirement(name = "bearerAuth")
    @CustomApiResponse(value = {
        CustomResponseCode.UNAUTHENTICATED_ERROR,
        CustomResponseCode.INVALID_TOKEN_ERROR,
        CustomResponseCode.DB_ERROR,
        CustomResponseCode.SYSTEM_ERROR
    })

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CS_ADMIN', 'SALES_ADMIN')")
    @DeleteMapping("/admin-sessions/current")
    public ResponseEntity<GlobalResponseDTO<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        long adminId = Long.parseLong(authentication.getName());

        adminAuthService.logout(request, response, adminId);

        return ResponseEntity.ok(GlobalResponseDTO.success());
    }

    @Operation(summary = "관리자 토큰 재발급 처리")
    @SecurityRequirement(name = "adminRefreshCookie")
    @CustomApiResponse(value = {
        CustomResponseCode.INVALID_TOKEN_ERROR,
        CustomResponseCode.DB_ERROR,
        CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping("/admin-token-refreshes")
    public ResponseEntity<GlobalResponseDTO<AdminAuthResponse>> reissue(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AdminAuthResponse authResponse = adminAuthService.reissue(request, response);
        return ResponseEntity.ok(GlobalResponseDTO.success(authResponse));
    }
}
