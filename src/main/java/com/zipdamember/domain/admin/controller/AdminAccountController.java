package com.zipdamember.domain.admin.controller;

import com.zipdamember.domain.admin.request.AdminAccountCreateRequest;
import com.zipdamember.domain.admin.request.AdminAccountSearchRequest;
import com.zipdamember.domain.admin.response.AdminAccountCreateResponse;
import com.zipdamember.domain.admin.response.AdminAccountListResponse;
import com.zipdamember.domain.admin.service.AdminAccountService;
import com.zipdamember.global.config.openapi.CustomApiResponse;
import com.zipdamember.global.response.GlobalResponseDTO;
import com.zipdamember.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 계정·권한 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/admin/admins")
public class AdminAccountController {
    private final AdminAccountService adminAccountService;

    @Operation(summary = "관리자 계정 생성", description = "최초 역할을 포함한 관리자 계정을 생성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @CustomApiResponse(value = {
        CustomResponseCode.DUPLICATED_RESOURCE_ERROR,
        CustomResponseCode.INVALID_PARAMETER_ERROR,
        CustomResponseCode.UNAUTHENTICATED_ERROR,
        CustomResponseCode.UNAUTHORIZED_ERROR,
        CustomResponseCode.DB_ERROR,
        CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<GlobalResponseDTO<AdminAccountCreateResponse>> create(
            @Valid @RequestBody AdminAccountCreateRequest request,
            HttpServletRequest httpServletRequest,
            Authentication authentication
    ) {
        // 요청 관리자 식별자 추출
        Long operatorAdminId = Long.parseLong(authentication.getName());

        // 관리자 계정 생성 처리
        AdminAccountCreateResponse response = adminAccountService.create(
                request,
                operatorAdminId,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(GlobalResponseDTO.success(response));
    }

    @Operation(summary = "관리자 계정·권한 목록 조회")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<GlobalResponseDTO<AdminAccountListResponse>> search(
        @Valid @ModelAttribute AdminAccountSearchRequest request
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(adminAccountService.search(request)));
    }
}
