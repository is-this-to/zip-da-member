package com.zipdamember.global.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(
    boolean secure
    , String issuer
    , String type
    , int accessTokenExpiryMs
    , int adminAccessTokenExpiryMs
    , int refreshTokenExpiryMs
    , String refreshTokenCookieName
    , String adminRefreshTokenCookieName
    , int refreshTokenCookieMaxAgeSeconds
    , String secret
    , String headerKey
    , String scheme
    , String refreshTokenCookiePath
    , String adminRefreshTokenCookiePath
) {}
