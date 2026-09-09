package com.zipdamember.domain.admin.controller;

import com.zipdamember.domain.admin.request.AdminAccountSearchRequest;
import com.zipdamember.domain.admin.response.AdminAccountListResponse;
import com.zipdamember.domain.admin.service.AdminAccountService;
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

@Tag(name = "관리자 계정·권한 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/admin/admins")
public class AdminAccountController {
    private final AdminAccountService adminAccountService;

    @Operation(summary = "관리자 계정·권한 목록 조회")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<GlobalResponseDTO<AdminAccountListResponse>> search(
        @Valid @ModelAttribute AdminAccountSearchRequest request
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(adminAccountService.search(request)));
    }
}
