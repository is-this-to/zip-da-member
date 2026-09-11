package com.zipdamember.global.security.oauth2;

import com.zipdamember.domain.auth.service.KakaoOAuth2UserService;
import com.zipdamember.global.response.constant.CustomResponseCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DelegatingOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final KakaoOAuth2UserService kakaoOAuth2UserService;

    @Override
    public OAuth2User loadUser(@NonNull OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        if ("kakao".equals(userRequest.getClientRegistration().getRegistrationId())) {
            return kakaoOAuth2UserService.loadUser(userRequest);
        }
        throw new OAuth2AuthenticationException(new OAuth2Error(
                CustomResponseCode.UNSUPPORTED_PROVIDER_ERROR.getCode(),
                "지원하지 않는 소셜 로그인 제공자입니다.",
                null
        ));
    }
}
