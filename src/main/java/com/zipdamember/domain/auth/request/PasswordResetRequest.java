package com.zipdamember.domain.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordResetRequest(
        @NotBlank String token,
        @NotBlank @Pattern(regexp = "^[0-9a-zA-Z!@#$]{8,20}$") String newPassword,
        @NotBlank String newPasswordCheck
) {
}
