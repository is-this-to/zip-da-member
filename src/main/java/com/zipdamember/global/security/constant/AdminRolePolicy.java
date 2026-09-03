package com.zipdamember.global.security.constant;

public enum AdminRolePolicy {
    SUPER_ADMIN("최고 관리자"),
    CS_ADMIN("CS관리자"),
    SALES_ADMIN("영업관리자"),
    SYSTEM("시스템");

    private final String description;

    AdminRolePolicy(String description) {
        this.description = description;
    }
}
