package com.zipdamember.global.security.oauth2;

import com.zipdamember.global.config.oauth.OAuth2Config;
import com.zipdamember.global.jwt.JwtConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SocialSignupCookieManager {

    private static final String COOKIE_PATH = "/api/member/auth";

    private final OAuth2Config oAuth2Config;
    private final JwtConfig jwtConfig;

    public void set(HttpServletResponse response, String token) {
        addCookie(response, token, oAuth2Config.socialSignupTokenExpirySeconds());
    }

    public Optional<String> get(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> oAuth2Config.socialSignupCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public void remove(HttpServletResponse response) {
        addCookie(response, null, 0);
    }

    private void addCookie(HttpServletResponse response, String value, int maxAge) {
        Cookie cookie = new Cookie(oAuth2Config.socialSignupCookieName(), value);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(true);
        cookie.setSecure(jwtConfig.secure());
        response.addCookie(cookie);
    }
}
