package com.zipdamember.domain.verification.controller;

import com.zipdamember.domain.verification.request.EmailVerificationRequest;
import com.zipdamember.domain.verification.request.ValidateMemberRequest;
import com.zipdamember.domain.verification.request.VerifyEmailVerificationRequest;
import com.zipdamember.domain.verification.response.EmailVerificationResponse;
import com.zipdamember.domain.verification.response.RegistrationDuplicateResponse;
import com.zipdamember.domain.verification.response.VerifyEmailVerificationResponse;
import com.zipdamember.domain.verification.service.VerificationService;
import com.zipdamember.global.response.GlobalResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class VerificationController {

    private final VerificationService verificationService;

    @PreAuthorize("!isAuthenticated()")
    @PostMapping("/member-validations")
    public ResponseEntity<GlobalResponseDTO<RegistrationDuplicateResponse>> checkDuplicate(@Valid @RequestBody ValidateMemberRequest validateMemberRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(verificationService.checkDuplicate(validateMemberRequest)));
    }

    @PreAuthorize("!isAuthenticated()")
    @PostMapping("/email-verifications")
    public ResponseEntity<GlobalResponseDTO<EmailVerificationResponse>> sendVerificationCode(@Valid @RequestBody EmailVerificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponseDTO.success(verificationService.sendVerificationCode(request)));
    }

    @PreAuthorize("!isAuthenticated()")
    @PatchMapping("/email-verifications/{verificationId}")
    public ResponseEntity<GlobalResponseDTO<VerifyEmailVerificationResponse>> verifyVerificationCode(
            @PathVariable Long verificationId,
            @Valid @RequestBody VerifyEmailVerificationRequest request
    ) {
        return ResponseEntity.ok(
                GlobalResponseDTO.success(verificationService.verifyVerificationCode(verificationId, request)));
    }
}
