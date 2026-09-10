package com.zipdamember.domain.agent.controller;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.agent.request.AdminAgentOperatingStatusChangeRequest;
import com.zipdamember.domain.agent.request.AdminAgentOperatingStatusSearchRequest;
import com.zipdamember.domain.agent.request.AdminAgentProfileSearchRequest;
import com.zipdamember.domain.agent.response.AdminAgentOperatingStatusListResponse;
import com.zipdamember.domain.agent.response.AdminAgentOperatingStatusResponse;
import com.zipdamember.domain.agent.response.AdminAgentProfileListResponse;
import com.zipdamember.domain.agent.service.AgentProfileService;
import com.zipdamember.global.openapi.CustomApiResponse;
import com.zipdamember.global.response.GlobalResponseDTO;
import com.zipdamember.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Tag(name = "관리자 중개소 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/admin/agencies")
public class AdminAgentProfileController {
    private final AgentProfileService agentProfileService;

    @Operation(summary = "중개소 검색·조회")
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SALES_ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<GlobalResponseDTO<AdminAgentProfileListResponse>> search(
            @Valid @ModelAttribute AdminAgentProfileSearchRequest request
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(agentProfileService.search(request)));
    }

    @Operation(summary = "중개소 영업 상태 목록 조회")
    @PreAuthorize("hasAnyRole('SALES_ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/operating-statuses")
    public ResponseEntity<GlobalResponseDTO<AdminAgentOperatingStatusListResponse>> searchOperatingStatuses(
            @Valid @ModelAttribute AdminAgentOperatingStatusSearchRequest request
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                agentProfileService.searchOperatingStatuses(request)
        ));
    }

    @Operation(summary = "중개소 영업 상태 변경")
    @CustomApiResponse(value = {
            CustomResponseCode.INVALID_PARAMETER_ERROR,
            CustomResponseCode.NOT_FOUND_RESOURCE_ERROR,
            CustomResponseCode.UNAUTHENTICATED_ERROR,
            CustomResponseCode.UNAUTHORIZED_ERROR,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('SALES_ADMIN', 'SUPER_ADMIN')")
    @PatchMapping("/{agentId}/operating-status")
    public ResponseEntity<GlobalResponseDTO<AdminAgentOperatingStatusResponse>> changeOperatingStatus(
            @PathVariable Long agentId,
            @Valid @RequestBody AdminAgentOperatingStatusChangeRequest request,
            Authentication authentication,
            jakarta.servlet.http.HttpServletRequest httpServletRequest
    ) {
        // 요청 관리자 식별자
        Long operatorAdminId = Long.parseLong(authentication.getName());

        // 요청 관리자 역할
        AdminRoleCode operatorRole = authentication.getAuthorities().stream()
                .map(authority -> AdminRoleCode.fromSecurityAuthority(authority.getAuthority()))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("관리자 역할 정보를 찾을 수 없습니다."));

        // 중개소 영업 상태 변경
        AdminAgentOperatingStatusResponse response = agentProfileService.changeOperatingStatus(
                agentId,
                request,
                operatorAdminId,
                operatorRole,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(GlobalResponseDTO.success(response));
    }
}
