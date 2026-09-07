package com.zipdamember.domain.auth.response;

import com.zipdamember.global.security.constant.MemberRolePolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record LoginResponse(
    @Schema(description = "회원 식별자")
    String memberId,

    @Schema(description = "현재 회원 역할")
    MemberRolePolicy role,

    @Schema(description = "회원 Access Token")
    String accessToken,

    @Schema(description = "Access Token 만료 시각")
    OffsetDateTime accessTokenExpiresAt
) {}
