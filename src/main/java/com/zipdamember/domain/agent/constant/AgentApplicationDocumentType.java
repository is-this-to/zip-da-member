package com.zipdamember.domain.agent.constant;

public enum AgentApplicationDocumentType {
    BUSINESS_LICENSE("사업자등록증"),
    BROKER_OFFICE_LICENSE("공인중개사무소 등록증");

    private final String description;

    AgentApplicationDocumentType(String description) {
        this.description = description;
    }
}
