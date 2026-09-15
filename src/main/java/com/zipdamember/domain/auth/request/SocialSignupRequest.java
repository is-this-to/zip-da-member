package com.zipdamember.domain.auth.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SocialSignupRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 50, message = "이름은 2~50자여야 합니다.")
        String name,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9_]{2,10}$", message = "닉네임은 한글, 영문, 숫자, 밑줄만 사용할 수 있습니다.")
        String nickname,

        @NotBlank(message = "휴대전화 번호는 필수입니다.")
        @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 휴대전화 번호 형식이 아닙니다.")
        String phone,

        @NotEmpty(message = "약관 동의 내역은 필수입니다.")
        List<@Valid TermAgreementRequest> termsAgreements
) {
}
