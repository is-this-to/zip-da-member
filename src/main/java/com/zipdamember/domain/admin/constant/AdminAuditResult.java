package com.zipdamember.domain.admin.constant;

public enum AdminAuditResult {
    SUCCESS("SUCCESS"),
    FAILURE("FAILURE"),
    DENIED("DENIED");

    private final String description;

    AdminAuditResult(String description) {
        this.description = description;
    }
}
