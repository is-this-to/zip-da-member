package com.zipdamember.domain.auth.controller;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.auth.request.AdminLoginRequest;
import com.zipdamember.domain.auth.request.AdminPasswordChangeRequest;
import com.zipdamember.domain.auth.response.AdminAuthResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
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

    @Operation(summary = "관리자 최초 비밀번호 변경", description = "최초 로그인 후 새 비밀번호를 설정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @CustomApiResponse(value = {
        CustomResponseCode.INVALID_PARAMETER_ERROR,
        CustomResponseCode.UNAUTHENTICATED_ERROR,
        CustomResponseCode.UNAUTHORIZED_ERROR,
        CustomResponseCode.INVALID_TOKEN_ERROR,
        CustomResponseCode.DB_ERROR,
        CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CS_ADMIN', 'SALES_ADMIN')")
    @PatchMapping("/admin-passwords/current")
    public ResponseEntity<GlobalResponseDTO<Void>> changeInitialPassword(
            @Valid @RequestBody AdminPasswordChangeRequest request,
            HttpServletRequest httpServletRequest,
            Authentication authentication
    ) {
        // 요청 관리자 식별자 추출
        Long adminId = Long.parseLong(authentication.getName());

        // 요청 관리자 역할 추출
        AdminRoleCode actorRole = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.equals("ROLE_SUPER_ADMIN")
                        || authority.equals("ROLE_CS_ADMIN")
                        || authority.equals("ROLE_SALES_ADMIN"))
                .map(authority -> AdminRoleCode.valueOf(authority.substring("ROLE_".length())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("관리자 역할 정보를 찾을 수 없습니다."));

        // 최초 비밀번호 변경 처리
        adminAuthService.changeInitialPassword(
                adminId,
                actorRole,
                request,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(GlobalResponseDTO.success());
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
