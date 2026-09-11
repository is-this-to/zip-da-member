package com.zipdamember.domain.member.controller;

import com.zipdamember.domain.member.request.AdminMemberSearchRequest;
import com.zipdamember.domain.member.response.AdminMemberListResponse;
import com.zipdamember.domain.member.service.AdminMemberService;
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

@Tag(name = "관리자 회원 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/admin/members")
public class AdminMemberController {
    private final AdminMemberService adminMemberService;

    @Operation(summary = "회원 검색·목록", description = "탈퇴 회원 포함, 개인정보 마스킹")
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<GlobalResponseDTO<AdminMemberListResponse>> search(
            @Valid @ModelAttribute AdminMemberSearchRequest request
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(adminMemberService.search(request)));
    }
}
