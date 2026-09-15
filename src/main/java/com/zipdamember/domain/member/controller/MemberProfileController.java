package com.zipdamember.domain.member.controller;

import com.zipdamember.domain.member.request.MemberEmailVerificationRequest;
import com.zipdamember.domain.member.request.MemberPasswordChangeRequest;
import com.zipdamember.domain.member.request.MemberProfileUpdateRequest;
import com.zipdamember.domain.member.response.MemberProfileResponse;
import com.zipdamember.domain.member.service.MemberProfileService;
import com.zipdamember.domain.verification.response.EmailVerificationResponse;
import com.zipdamember.domain.verification.response.VerifyEmailVerificationResponse;
import com.zipdamember.domain.verification.util.VerificationIdParser;
import com.zipdamember.global.response.GlobalResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import com.zipdamember.global.cookie.CookieManager;

@Tag(name = "마이페이지 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/members/me")
@PreAuthorize("hasAnyRole('USER', 'AGENT')")
public class MemberProfileController {
    private final MemberProfileService memberProfileService;
    private final CookieManager cookieManager;

    @Operation(summary = "내 프로필 조회")
    @GetMapping
    public ResponseEntity<GlobalResponseDTO<MemberProfileResponse>> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                memberProfileService.getMyProfile(memberId(authentication))
        ));
    }

    @Operation(summary = "내 프로필 부분 수정")
    @PatchMapping
    public ResponseEntity<GlobalResponseDTO<MemberProfileResponse>> updateMyProfile(
            @Valid @RequestBody MemberProfileUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                memberProfileService.updateMyProfile(memberId(authentication), request)
        ));
    }

    @Operation(summary = "비밀번호 변경용 이메일 인증번호 발송")
    @PostMapping("/password-verifications")
    public ResponseEntity<GlobalResponseDTO<EmailVerificationResponse>> sendPasswordVerification(
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponseDTO.success(
                memberProfileService.sendPasswordVerification(memberId(authentication))
        ));
    }

    @Operation(summary = "비밀번호 변경용 이메일 인증번호 확인")
    @PatchMapping("/password-verifications/{verificationId}")
    public ResponseEntity<GlobalResponseDTO<VerifyEmailVerificationResponse>> verifyPasswordCode(
            @PathVariable String verificationId,
            @Valid @RequestBody MemberEmailVerificationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                memberProfileService.verifyPasswordCode(
                        memberId(authentication),
                        VerificationIdParser.parse(verificationId),
                        request
                )
        ));
    }

    @Operation(summary = "내 비밀번호 변경")
    @PatchMapping("/password")
    public ResponseEntity<GlobalResponseDTO<Void>> changePassword(
            @Valid @RequestBody MemberPasswordChangeRequest request,
            Authentication authentication,
            HttpServletResponse response
    ) {
        memberProfileService.changePassword(memberId(authentication), request);
        cookieManager.removeRefreshTokenToCookie(response);
        return ResponseEntity.ok(GlobalResponseDTO.success());
    }

    private Long memberId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
