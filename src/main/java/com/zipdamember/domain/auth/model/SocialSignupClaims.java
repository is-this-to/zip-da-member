package com.zipdamember.domain.auth.model;

import com.zipdamember.global.security.constant.ProviderPolicy;

public record SocialSignupClaims(
        ProviderPolicy provider,
        String providerUserId,
        String email,
        String nickname,
        String profileImageUrl
) {
}
