package com.zipdamember.domain.auth.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record SocialAccountLinkRequest(
        @NotBlank(message = "기존 계정 비밀번호는 필수입니다.")
        String password,

        @AssertTrue(message = "소셜 계정 연결에 동의해야 합니다.")
        boolean agreed
) {
}
