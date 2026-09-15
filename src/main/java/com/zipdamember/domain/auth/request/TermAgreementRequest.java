package com.zipdamember.domain.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TermAgreementRequest(
        @NotBlank(message = "약관 식별자는 필수입니다.")
        @Pattern(regexp = "^[1-9]\\d*$", message = "약관 식별자는 양수여야 합니다.")
        String termsId,

        @NotBlank(message = "약관 버전은 필수입니다.")
        @Size(max = 10, message = "약관 버전은 10자 이하여야 합니다.")
        String version,

        @NotNull(message = "약관 동의 여부는 필수입니다.")
        Boolean agreed
) {
}
