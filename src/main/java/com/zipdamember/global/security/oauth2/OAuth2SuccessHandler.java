package com.zipdamember.global.security.oauth2;

import com.zipdamember.domain.auth.model.SocialSignupClaims;
import com.zipdamember.domain.auth.service.SocialAuthService;
import com.zipdamember.global.config.oauth.OAuth2Config;
import com.zipdamember.global.security.constant.ProviderPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SocialAuthService socialAuthService;
    private final SocialSignupTokenProvider socialSignupTokenProvider;
    private final SocialSignupCookieManager socialSignupCookieManager;
    private final OAuth2Config oAuth2Config;

    @Override
    public void onAuthenticationSuccess(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Authentication authentication
    ) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String flow = (String) attributes.get("flow");

        if ("LOGIN".equals(flow)) {
            socialSignupCookieManager.remove(response);
            socialAuthService.loginExistingMember(request, response, (Long) attributes.get("memberId"));
            redirect(request, response, "login");
            return;
        }

        SocialSignupClaims claims = new SocialSignupClaims(
                ProviderPolicy.valueOf((String) attributes.get("provider")),
                (String) attributes.get("providerUserId"),
                (String) attributes.get("email"),
                (String) attributes.get("nickname"),
                (String) attributes.get("profileImageUrl")
        );
        socialSignupCookieManager.set(response, socialSignupTokenProvider.generate(claims));
        redirect(request, response, "LINK".equals(flow) ? "link" : "signup");
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, String flow) throws IOException {
        String redirectUri = UriComponentsBuilder
                .fromUri(URI.create(oAuth2Config.frontendCallbackUri()))
                .queryParam("flow", flow)
                .build()
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }
}
