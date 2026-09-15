package com.zipdamember.domain.auth.service;

import com.zipdamember.domain.auth.entity.SocialAccount;
import com.zipdamember.domain.auth.repository.SocialAccountRepository;
import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.global.response.constant.CustomResponseCode;
import com.zipdamember.global.security.constant.ProviderPolicy;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final SocialAccountRepository socialAccountRepository;
    private final MemberAccountRepository memberAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public OAuth2User loadUser(@NonNull OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User kakaoUser = new DefaultOAuth2UserService().loadUser(userRequest);
        Map<String, Object> attributes = kakaoUser.getAttributes();
        String providerUserId = String.valueOf(attributes.get("id"));

        Map<String, Object> kakaoAccount = mapValue(attributes, "kakao_account");
        String email = stringValue(kakaoAccount, "email").trim().toLowerCase(Locale.ROOT);
        if (!Boolean.TRUE.equals(kakaoAccount.get("is_email_valid"))
                || !Boolean.TRUE.equals(kakaoAccount.get("is_email_verified"))) {
            throw oauthError(CustomResponseCode.OAUTH2_ERROR, "카카오에서 인증된 이메일을 제공받지 못했습니다.");
        }

        Map<String, Object> profile = mapValue(kakaoAccount, "profile");
        String nickname = stringValue(profile, "nickname");
        String profileImageUrl = null;
        if (!Boolean.TRUE.equals(kakaoAccount.get("profile_image_needs_agreement"))) {
            profileImageUrl = nullableString(profile.get("profile_image_url"));
        }

        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(ProviderPolicy.KAKAO, providerUserId)
                .orElse(null);

        Map<String, Object> principal = new HashMap<>();
        principal.put("providerUserId", providerUserId);
        principal.put("provider", ProviderPolicy.KAKAO.name());

        if (socialAccount != null) {
            MemberAccount member = memberAccountRepository.findById(socialAccount.getMemberId())
                    .orElseThrow(() -> oauthError(CustomResponseCode.OAUTH2_ERROR, "연결된 회원 정보를 찾을 수 없습니다."));
            if (member.getStatus() != MemberStatus.ACTIVE || member.getWithdrawnAt() != null) {
                throw oauthError(CustomResponseCode.UNAUTHORIZED_ERROR, "현재 로그인할 수 없는 회원입니다.");
            }
            principal.put("flow", "LOGIN");
            principal.put("memberId", member.getMemberId());
            return new DefaultOAuth2User(
                    List.of(new SimpleGrantedAuthority("ROLE_" + member.getMemberRole().name())),
                    principal,
                    "providerUserId"
            );
        }

        principal.put("flow", memberAccountRepository.existsByEmail(email) ? "LINK" : "SIGNUP");
        principal.put("email", email);
        principal.put("nickname", nickname);
        if (profileImageUrl != null) {
            principal.put("profileImageUrl", profileImageUrl);
        }
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_GUEST")),
                principal,
                "providerUserId"
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof Map<?, ?>)) {
            throw oauthError(CustomResponseCode.OAUTH2_ERROR, "카카오 필수 사용자 정보가 없습니다.");
        }
        return (Map<String, Object>) value;
    }

    private String stringValue(Map<String, Object> source, String key) {
        String value = nullableString(source.get(key));
        if (value == null || value.isBlank()) {
            throw oauthError(CustomResponseCode.OAUTH2_ERROR, "카카오 필수 사용자 정보가 없습니다.");
        }
        return value;
    }

    private String nullableString(Object value) {
        return value instanceof String string ? string : null;
    }

    private OAuth2AuthenticationException oauthError(CustomResponseCode code, String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(code.getCode(), message, null));
    }
}
