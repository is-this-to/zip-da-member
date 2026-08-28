package com.zipdamember.domain.verification.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "이메일 가입 유효성 검사")
public record EmailDuplicateCheckRequest(
        @Schema(description = "이메일", example = "gooood@test.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 255, message = "이메일은 255자 이하로 입력해야 합니다.")
        String email
) {
}
