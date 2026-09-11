package com.zipdamember.domain.member.constant;

public enum MemberSanctionScope {
    ACCOUNT("계정"),
    PROPERTY("매물");

    private final String description;

    MemberSanctionScope(String description) {
        this.description = description;
    }
}
