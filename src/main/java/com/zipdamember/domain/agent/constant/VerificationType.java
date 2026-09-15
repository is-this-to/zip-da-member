package com.zipdamember.domain.agent.constant;

public enum VerificationType {
    VALIDATION("진위 확인"),
    STATUS("상태 조회");

    private final String description;

    VerificationType(String description) {
        this.description = description;
    }
}
