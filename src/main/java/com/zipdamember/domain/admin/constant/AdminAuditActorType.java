package com.zipdamember.domain.admin.constant;

public enum AdminAuditActorType {
    ADMIN("ADMIN"),
    SYSTEM("SYSTEM");

    private final String description;

    AdminAuditActorType(String description) {
        this.description = description;
    }
}
