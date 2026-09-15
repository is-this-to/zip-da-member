package com.zipdamember.domain.member.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MemberPasswordChangeRequest(
        @NotBlank String currentPassword,
        @NotBlank String verificationId,
        @NotBlank @Pattern(regexp = "^[0-9a-zA-Z!@#$]{8,20}$") String newPassword,
        @NotBlank String newPasswordCheck
) {
}
