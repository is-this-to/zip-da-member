package com.zipdamember.global.security.constant;

import lombok.Getter;

@Getter
public enum MemberRolePolicy {
    USER("일반 사용자"),
    AGENT("공인중개사");

    private final String memberRoleDescription;

    MemberRolePolicy(String memberRoleDescription) {
        this.memberRoleDescription=memberRoleDescription;
    }
}