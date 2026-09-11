package com.zipdamember.global.config.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zipda.oauth2")
public record OAuth2Config(
        String frontendCallbackUri,
        String socialSignupCookieName,
        int socialSignupTokenExpirySeconds
) {
}
