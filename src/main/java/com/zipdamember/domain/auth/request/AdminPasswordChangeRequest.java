package com.zipdamember.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminPasswordChangeRequest(
        @Schema(description = "새 관리자 비밀번호", example = "NewPassword1!")
        @NotBlank
        @Pattern(regexp = "^[0-9a-zA-Z!@#$]{8,20}$")
        String newPassword,

        @Schema(description = "새 관리자 비밀번호 확인", example = "NewPassword1!")
        @NotBlank
        @Pattern(regexp = "^[0-9a-zA-Z!@#$]{8,20}$")
        String newPasswordConfirm
) {
}
