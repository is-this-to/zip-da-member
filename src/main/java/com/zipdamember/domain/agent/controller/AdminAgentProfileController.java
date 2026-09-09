package com.zipdamember.domain.agent.controller;

import com.zipdamember.domain.agent.request.AdminAgentProfileSearchRequest;
import com.zipdamember.domain.agent.response.AdminAgentProfileListResponse;
import com.zipdamember.domain.agent.service.AgentProfileService;
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
}
