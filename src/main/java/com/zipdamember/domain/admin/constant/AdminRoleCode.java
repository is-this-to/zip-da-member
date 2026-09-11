package com.zipdamember.domain.admin.constant;

import java.util.Arrays;
import java.util.Optional;

public enum AdminRoleCode {
    SUPER_ADMIN("최고 관리자"),
    CS_ADMIN("CS관리자"),
    SALES_ADMIN("영업관리자"),
    SYSTEM("시스템");

    private final String description;

    AdminRoleCode(String description) {
        this.description = description;
    }

    public static Optional<AdminRoleCode> fromSecurityAuthority(String authority) {
        return Arrays.stream(values())
                .filter(role -> role != SYSTEM)
                .filter(role -> ("ROLE_" + role.name()).equals(authority))
                .findFirst();
    }
}
