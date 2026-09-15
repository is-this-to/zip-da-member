package com.zipdamember.domain.agent.constant;

public enum AgentApplicationStatus {
    PENDING("대기"),
    UNDER_REVIEW("심사 중"),
    APPROVED("승인"),
    REJECTED("보완 요청"),
    INCORRECT_DATA("잘못된 자료");

    private final String description;

    AgentApplicationStatus(String description) {
        this.description = description;
    }
}
