package com.zipdamember.domain.verification.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "이메일 인증번호 확인 요청")
public record VerifyEmailVerificationRequest(

        @Schema(
                description = "인증번호를 발송한 이메일",
                example = "user@gmail.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "이메일은 필수 항목입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @Schema(
                description = "이메일로 받은 6자리 인증번호",
                example = "381920",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "인증번호는 필수 항목입니다.")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "인증번호는 6자리 숫자여야 합니다."
        )
        String verificationCode
) {}
