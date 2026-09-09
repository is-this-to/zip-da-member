package com.zipdamember.domain.agent.controller;

import com.zipdamember.domain.agent.request.AdminAgentApplicationSearchRequest;
import com.zipdamember.domain.agent.response.AdminAgentApplicationListResponse;
import com.zipdamember.domain.agent.service.AdminAgentApplicationService;
import com.zipdamember.global.response.GlobalResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
