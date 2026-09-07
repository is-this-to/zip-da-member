package com.zipdamember.domain.auth.response;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminAuthResponse(
        @Schema(description = "관리자 식별자", example = "471234567890123481")
        String adminId,

        @Schema(description = "현재 활성 관리자 역할")
        List<AdminRoleCode> roles,

        @Schema(description = "최초 로그인 비밀번호 변경 필요 여부")
        boolean passwordChangeRequired,

        @Schema(description = "관리자 Access Token")
        String accessToken,

        @Schema(description = "Access Token 만료 시각")
        OffsetDateTime accessTokenExpiresAt
) {
    public AdminAuthResponse {
        roles = List.copyOf(roles);
    }
}
