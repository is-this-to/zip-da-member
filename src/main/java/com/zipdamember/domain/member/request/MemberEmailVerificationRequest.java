package com.zipdamember.domain.member.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MemberEmailVerificationRequest(
        @NotBlank @Pattern(regexp = "^\\d{6}$") String verificationCode
) {
}
