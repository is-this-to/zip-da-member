package com.zipdamember.domain.verification.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "이메일 인증 시 필요 데이터")
public record EmailVerificationRequest(
        @Schema(description = "인증 이메일", example = "jiyoon040114@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "이메일은 필수 항목입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email
) {
}
