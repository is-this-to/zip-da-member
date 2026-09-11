package com.zipdamember.global.security.constant;

import lombok.Getter;

@Getter
public enum ProviderPolicy {
    NONE("일반 가입 사용자"),
    KAKAO("카카오 소셜 사용자");

    private final String providerDescription;

    ProviderPolicy(String providerDescription) {
        this.providerDescription=providerDescription;
    }
}
