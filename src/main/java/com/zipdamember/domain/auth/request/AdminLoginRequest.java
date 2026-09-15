package com.zipdamember.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
        @Schema(description = "관리자 로그인 코드", example = "admin001")
        @NotBlank
        @Size(max = 20)
        String adminCode,

        @Schema(description = "관리자 비밀번호. 최초 비밀번호 변경 대상은 생략 가능", example = "admin-password")
        @Size(max = 20)
        String adminPassword
) {
}
