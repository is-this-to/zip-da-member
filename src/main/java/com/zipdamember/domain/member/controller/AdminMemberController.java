package com.zipdamember.domain.member.controller;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.member.request.AdminMemberSearchRequest;
import com.zipdamember.domain.member.request.AdminMemberSuspendReleaseRequest;
import com.zipdamember.domain.member.request.AdminMemberSuspendRequest;
import com.zipdamember.domain.member.response.AdminMemberListResponse;
import com.zipdamember.domain.member.response.AdminMemberSanctionHistoryResponse;
import com.zipdamember.domain.member.service.AdminMemberSanctionService;
import com.zipdamember.domain.member.service.AdminMemberService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 회원 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/admin/members")
public class AdminMemberController {
    private final AdminMemberService adminMemberService;
    private final AdminMemberSanctionService adminMemberSanctionService;

    @Operation(summary = "회원 검색·목록", description = "탈퇴 회원 포함, 개인정보 마스킹")
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<GlobalResponseDTO<AdminMemberListResponse>> search(
            @Valid @ModelAttribute AdminMemberSearchRequest request
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(adminMemberService.search(request)));
    }

    @Operation(summary = "회원 제재·해제 이력 조회", description = "회원별 제재·해제 이력을 최신순으로 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @CustomApiResponse(value = {
            CustomResponseCode.UNAUTHENTICATED_ERROR,
            CustomResponseCode.UNAUTHORIZED_ERROR,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/{memberId}/sanction-histories")
    public ResponseEntity<GlobalResponseDTO<AdminMemberSanctionHistoryResponse>> getSanctionHistories(
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                adminMemberSanctionService.getHistories(memberId)
        ));
    }

    @Operation(summary = "회원 정지", description = "회원 범위·기간·사유를 지정하여 제재합니다.")
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
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/{memberId}/sanctions")
    public ResponseEntity<GlobalResponseDTO<Void>> suspend(
            @PathVariable Long memberId,
            @Valid @RequestBody AdminMemberSuspendRequest request,
            HttpServletRequest httpServletRequest,
            Authentication authentication
    ) {
        // 요청 관리자 식별자 추출
        Long operatorAdminId = Long.parseLong(authentication.getName());

        // 요청 관리자 역할 추출
        AdminRoleCode operatorRole = resolveOperatorRole(authentication);

        // 회원 정지 처리
        adminMemberSanctionService.suspend(
                memberId,
                request,
                operatorAdminId,
                operatorRole,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(GlobalResponseDTO.success());
    }

    @Operation(summary = "회원 정지 해제", description = "활성 상태의 회원 제재를 해제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @CustomApiResponse(value = {
            CustomResponseCode.INVALID_PARAMETER_ERROR,
            CustomResponseCode.NOT_FOUND_RESOURCE_ERROR,
            CustomResponseCode.UNAUTHENTICATED_ERROR,
            CustomResponseCode.UNAUTHORIZED_ERROR,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    @PatchMapping("/sanctions/{sanctionId}/release")
    public ResponseEntity<GlobalResponseDTO<Void>> release(
            @PathVariable Long sanctionId,
            @Valid @RequestBody AdminMemberSuspendReleaseRequest request,
            HttpServletRequest httpServletRequest,
            Authentication authentication
    ) {
        // 요청 관리자 식별자 추출
        Long operatorAdminId = Long.parseLong(authentication.getName());

        // 요청 관리자 역할 추출
        AdminRoleCode operatorRole = resolveOperatorRole(authentication);

        // 회원 정지 해제 처리
        adminMemberSanctionService.release(
                sanctionId,
                request,
                operatorAdminId,
                operatorRole,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(GlobalResponseDTO.success());
    }

    private AdminRoleCode resolveOperatorRole(Authentication authentication) {
        // 최고 관리자 역할 우선 선택
        if (hasAuthority(authentication, AdminRoleCode.SUPER_ADMIN)) {
            return AdminRoleCode.SUPER_ADMIN;
        }

        // CS 관리자 역할 선택
        if (hasAuthority(authentication, AdminRoleCode.CS_ADMIN)) {
            return AdminRoleCode.CS_ADMIN;
        }

        throw new AccessDeniedException("회원 관리 권한이 없습니다.");
    }

    private boolean hasAuthority(Authentication authentication, AdminRoleCode roleCode) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> AdminRoleCode.fromSecurityAuthority(authority)
                        .filter(roleCode::equals)
                        .isPresent());
    }
}
