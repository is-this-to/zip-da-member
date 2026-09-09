package com.zipdamember.global.security.oauth2;

import com.zipdamember.global.config.oauth.OAuth2Config;
import com.zipdamember.global.response.constant.CustomResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("^E\\d{2}$");

    private final OAuth2Config oAuth2Config;
    private final SocialSignupCookieManager socialSignupCookieManager;

    @Override
    public void onAuthenticationFailure(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AuthenticationException exception
    ) throws IOException {
        socialSignupCookieManager.remove(response);
        String code = CustomResponseCode.OAUTH2_ERROR.getCode();
        String message = "카카오 로그인에 실패했습니다.";

        if (exception instanceof OAuth2AuthenticationException oauthException) {
            String oauthCode = oauthException.getError().getErrorCode();
            if (ERROR_CODE_PATTERN.matcher(oauthCode).matches()) {
                code = oauthCode;
                message = oauthException.getError().getDescription();
            }
        }

        String redirectUri = UriComponentsBuilder
                .fromUri(URI.create(oAuth2Config.frontendCallbackUri()))
                .queryParam("code", code)
                .queryParam("message", message)
                .build()
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }
}
