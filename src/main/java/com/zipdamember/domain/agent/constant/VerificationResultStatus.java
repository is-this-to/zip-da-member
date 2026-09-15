package com.zipdamember.domain.agent.constant;

public enum VerificationResultStatus {
    MATCHED("일치"),
    MISMATCHED("불일치"),
    ERROR("오류");

    private final String description;

    VerificationResultStatus(String description) {
        this.description = description;
    }
}
