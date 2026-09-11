package com.zipdamember.domain.admin.constant;
//TODO: 삭제. golbal>security>constant AdminRolePolicy 사용하기
public enum AdminRoleCode {
    SUPER_ADMIN("최고 관리자"),
    CS_ADMIN("CS관리자"),
    SALES_ADMIN("영업관리자"),
    SYSTEM("시스템");

    private final String description;

    AdminRoleCode(String description) {
        this.description = description;
    }
}