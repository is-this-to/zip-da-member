package com.zipdamember.domain.member.constant;

public enum MemberSuspensionReasonCode {
    FALSE_PROPERTY_REPEAT("허위매물 반복 위반"),
    SUSPENSION_COUNT_LIMIT("계정 정지 5회 누적");

    private final String description;

    MemberSuspensionReasonCode(String description) {
        this.description = description;
    }
}
