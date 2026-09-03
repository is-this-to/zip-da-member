package com.zipdamember.domain.admin.constant;

public enum AdminLoginResult {
    SUCCESS("SUCCESS"),
    FAILURE("FAILURE"),
    BLOCKED("BLOCKED");

    private final String description;

    AdminLoginResult(String description) {
        this.description = description;
    }
}
