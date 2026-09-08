package com.zipdamember.domain.verification.controller;

import com.zipdamember.domain.verification.constant.EmailVerificationPurposePolicy;
import com.zipdamember.domain.verification.request.EmailVerificationRequest;
import com.zipdamember.domain.verification.request.ValidateMemberRequest;
import com.zipdamember.domain.verification.request.VerifyEmailVerificationRequest;
import com.zipdamember.domain.verification.response.EmailVerificationResponse;
import com.zipdamember.domain.verification.response.RegistrationDuplicateResponse;
import com.zipdamember.domain.verification.response.VerifyEmailVerificationResponse;
import com.zipdamember.domain.verification.service.VerificationService;
import com.zipdamember.global.config.openapi.CustomApiResponse;
import com.zipdamember.global.response.GlobalResponseDTO;
import com.zipdamember.global.response.constant.CustomResponseCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class VerificationController {

    private final VerificationService verificationService;

    /**
     * 이메일 또는 닉네임 중복 검사
     * @param validateMemberRequest 이메일 또는 닉네임을 보냄
     * @return 사용 가능 여부
     */
    @CustomApiResponse(value = {
            CustomResponseCode.INVALID_PARAMETER_ERROR,
            CustomResponseCode.UNAUTHORIZED_ERROR,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("!isAuthenticated()")
    @PostMapping("/member-validations")
    public ResponseEntity<GlobalResponseDTO<RegistrationDuplicateResponse>> checkDuplicate(@Valid @RequestBody ValidateMemberRequest validateMemberRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(verificationService.checkDuplicate(validateMemberRequest)));
    }

    /**
     * 인증 이메일 송신
     * @param request 인증 이메일
     * @return 이메일 인증 식별자, 만료 시간, 재수신 쿨타임
     */
    @CustomApiResponse(value = {
            CustomResponseCode.INVALID_PARAMETER_ERROR,
            CustomResponseCode.ALREADY_REGISTERED_ERROR,
            CustomResponseCode.EMAIL_RESEND_LIMIT_ERROR,
            CustomResponseCode.EMAIL_SEND_ERROR,
            CustomResponseCode.UNAUTHORIZED_ERROR,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.DB_DUPLICATED_KEY_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("!isAuthenticated()")
    @PostMapping("/email-verifications")
    public ResponseEntity<GlobalResponseDTO<EmailVerificationResponse>> sendVerificationCode(@Valid @RequestBody EmailVerificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponseDTO.success(verificationService.sendVerificationCode(request)));
    }

    /**
     * 인증 이메일 인증
     * @param verificationId 수신 이메일 코드 ID
    * @param request 사용자 입력 인증 코드
     * @return 이메일 인증번호 확인 응답
     */
    @CustomApiResponse(value = {
            CustomResponseCode.INVALID_PARAMETER_ERROR,
            CustomResponseCode.EMAIL_VERIFICATION_NOT_FOUND,
            CustomResponseCode.EMAIL_VERIFICATION_EMAIL_MISMATCH,
            CustomResponseCode.EMAIL_VERIFICATION_EXPIRED,
            CustomResponseCode.EMAIL_VERIFICATION_ALREADY_COMPLETED,
            CustomResponseCode.VERIFICATION_ATTEMPTS_EXCEEDED,
            CustomResponseCode.VERIFICATION_CODE_INVALID,
            CustomResponseCode.UNAUTHORIZED_ERROR,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("!isAuthenticated()")
    @PatchMapping("/email-verifications/{verificationId}")
    public ResponseEntity<GlobalResponseDTO<VerifyEmailVerificationResponse>> verifyVerificationCode(
            @PathVariable Long verificationId,
            @Valid @RequestBody VerifyEmailVerificationRequest request
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(verificationService.verifyVerificationCode(verificationId, request, EmailVerificationPurposePolicy.SIGNUP)));
    }
}
