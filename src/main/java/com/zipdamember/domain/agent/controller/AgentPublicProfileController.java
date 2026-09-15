package com.zipdamember.domain.agent.controller;

import com.zipdamember.domain.agent.request.AgentPublicProfileUpdateRequest;
import com.zipdamember.domain.agent.response.AgentPublicProfileResponse;
import com.zipdamember.domain.agent.service.AgentPublicProfileService;
import com.zipdamember.global.response.GlobalResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/agents")
public class AgentPublicProfileController {
    private final AgentPublicProfileService agentPublicProfileService;

    @GetMapping("/{agentId}")
    public ResponseEntity<GlobalResponseDTO<AgentPublicProfileResponse>> getPublicProfile(
            @PathVariable Long agentId
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                agentPublicProfileService.getPublicProfile(agentId)
        ));
    }

    @PreAuthorize("hasRole('AGENT')")
    @PatchMapping("/{agentId}")
    public ResponseEntity<GlobalResponseDTO<AgentPublicProfileResponse>> updateMyProfile(
            @PathVariable Long agentId,
            @Valid @RequestBody AgentPublicProfileUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                agentPublicProfileService.updateMyProfile(
                        agentId,
                        Long.parseLong(authentication.getName()),
                        request
                )
        ));
    }
}
