package com.zipdamember.domain.agent.constant;

public enum AgentOperatingStatus {
    ACTIVE("영업중"),
    SUSPENDED("휴업"),
    CLOSED("폐업");

    private final String description;

    AgentOperatingStatus(String description) {
        this.description = description;
    }
}
