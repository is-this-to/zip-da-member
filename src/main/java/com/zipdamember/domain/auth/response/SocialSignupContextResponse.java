package com.zipdamember.domain.auth.response;

public record SocialSignupContextResponse(
        String email,
        String nickname,
        String profileImageUrl
) {
}
