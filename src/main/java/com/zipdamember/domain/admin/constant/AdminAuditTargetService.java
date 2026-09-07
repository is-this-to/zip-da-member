package com.zipdamember.domain.admin.constant;

public enum AdminAuditTargetService {
    MEMBER("MEMBER"),
    PROPERTY("PROPERTY"),
    COMMUNITY("COMMUNITY");

    private final String description;

    AdminAuditTargetService(String description) {
        this.description = description;
    }
}
