package com.zipdamember.domain.admin.controller;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.admin.request.AdminRoleAssignRequest;
import com.zipdamember.domain.admin.service.AdminRoleAssignmentService;
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
@RequestMapping("/api/member/admin/admins/{adminId}/roles")
public class AdminRoleController {
    private final AdminRoleAssignmentService adminRoleAssignmentService;

    @Operation(summary = "관리자 역할 부여")
    @SecurityRequirement(name = "bearerAuth")
    @CustomApiResponse(value = {
        CustomResponseCode.DUPLICATED_RESOURCE_ERROR,
        CustomResponseCode.INVALID_PARAMETER_ERROR,
        CustomResponseCode.NOT_FOUND_RESOURCE_ERROR,
        CustomResponseCode.UNAUTHENTICATED_ERROR,
        CustomResponseCode.UNAUTHORIZED_ERROR,
        CustomResponseCode.DB_ERROR,
        CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<GlobalResponseDTO<Void>> assign(
            @PathVariable Long adminId,
            @Valid @RequestBody AdminRoleAssignRequest request,
            HttpServletRequest httpServletRequest,
            Authentication authentication
    ) {
        // 요청 관리자 식별자 추출
        Long operatorAdminId = Long.parseLong(authentication.getName());

        // 관리자 역할 부여 처리
        adminRoleAssignmentService.assign(
                adminId,
                request.roleCode(),
                operatorAdminId,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(GlobalResponseDTO.success());
    }

    @Operation(summary = "관리자 역할 회수")
    @SecurityRequirement(name = "bearerAuth")
    @CustomApiResponse(value = {
        CustomResponseCode.INVALID_PARAMETER_ERROR,
        CustomResponseCode.NOT_FOUND_RESOURCE_ERROR,
        CustomResponseCode.UNAUTHENTICATED_ERROR,
        CustomResponseCode.UNAUTHORIZED_ERROR,
        CustomResponseCode.DB_ERROR,
        CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{roleCode}")
    public ResponseEntity<GlobalResponseDTO<Void>> revoke(
            @PathVariable Long adminId,
            @PathVariable AdminRoleCode roleCode,
            HttpServletRequest httpServletRequest,
            Authentication authentication
    ) {
        // 요청 관리자 식별자 추출
        Long operatorAdminId = Long.parseLong(authentication.getName());

        // 관리자 역할 회수 처리
        adminRoleAssignmentService.revoke(
                adminId,
                roleCode,
                operatorAdminId,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(GlobalResponseDTO.success());
    }
}
