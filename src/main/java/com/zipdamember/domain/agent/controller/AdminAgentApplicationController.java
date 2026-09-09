package com.zipdamember.domain.agent.controller;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.agent.request.AdminAgentApplicationSearchRequest;
import com.zipdamember.domain.agent.request.AdminAgentApplicationSupplementRequest;
import com.zipdamember.domain.agent.response.AdminAgentApplicationDetailResponse;
import com.zipdamember.domain.agent.response.AdminAgentApplicationListResponse;
import com.zipdamember.domain.agent.response.AdminAgentApplicationSupplementResponse;
import com.zipdamember.domain.agent.service.AdminAgentApplicationService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Tag(name = "관리자 중개사 심사 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/admin/agent-applications")
public class AdminAgentApplicationController {

    private final AdminAgentApplicationService adminAgentApplicationService;

    @Operation(summary = "중개사 신청 검토 목록 조회")
    @PreAuthorize("hasAnyRole('SALES_ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<GlobalResponseDTO<AdminAgentApplicationListResponse>> search(
            @Valid @ModelAttribute AdminAgentApplicationSearchRequest request
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                adminAgentApplicationService.search(request)
        ));
    }

    @Operation(summary = "중개사 신청·서류 상세 조회")
    @CustomApiResponse(value = {
            CustomResponseCode.NOT_FOUND_RESOURCE_ERROR,
            CustomResponseCode.UNAUTHENTICATED_ERROR,
            CustomResponseCode.UNAUTHORIZED_ERROR,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('SALES_ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/{applicationId}")
    public ResponseEntity<GlobalResponseDTO<AdminAgentApplicationDetailResponse>> getDetail(
            @PathVariable Long applicationId
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                adminAgentApplicationService.getDetail(applicationId)
        ));
    }

    @Operation(summary = "중개사 신청 보완 요청")
    @CustomApiResponse(value = {
            CustomResponseCode.INVALID_PARAMETER_ERROR,
            CustomResponseCode.NOT_FOUND_RESOURCE_ERROR,
            CustomResponseCode.UNAUTHENTICATED_ERROR,
            CustomResponseCode.UNAUTHORIZED_ERROR,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('SALES_ADMIN', 'SUPER_ADMIN')")
    @PatchMapping("/{applicationId}/supplement-request")
    public ResponseEntity<GlobalResponseDTO<AdminAgentApplicationSupplementResponse>> requestSupplement(
            @PathVariable Long applicationId,
            @Valid @RequestBody AdminAgentApplicationSupplementRequest request,
            Authentication authentication,
            jakarta.servlet.http.HttpServletRequest httpServletRequest
    ) {
        // 요청 관리자 식별자
        Long reviewerAdminId = Long.parseLong(authentication.getName());

        // 요청 관리자 역할
        AdminRoleCode reviewerRole = authentication.getAuthorities().stream()
                .map(authority -> AdminRoleCode.fromSecurityAuthority(authority.getAuthority()))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("관리자 역할 정보를 찾을 수 없습니다."));

        // 중개사 신청 보완 요청
        return ResponseEntity.ok(GlobalResponseDTO.success(
                adminAgentApplicationService.requestSupplement(
                        applicationId,
                        request,
                        reviewerAdminId,
                        reviewerRole,
                        httpServletRequest.getRemoteAddr(),
                        httpServletRequest.getHeader("User-Agent")
                )
        ));
    }
}
