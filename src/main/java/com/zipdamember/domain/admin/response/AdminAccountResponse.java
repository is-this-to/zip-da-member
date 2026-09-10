package com.zipdamember.domain.admin.response;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.admin.entity.Admin;

import java.time.LocalDateTime;
import java.util.List;

public record AdminAccountResponse(
    String adminId,
    String adminCode,
    String adminName,
    List<AdminRoleCode> activeRoles,
    LocalDateTime createdAt,
    LocalDateTime lastLoginAt
) {
    public AdminAccountResponse {
        activeRoles = List.copyOf(activeRoles);
    }

    public static AdminAccountResponse of(
        Admin admin,
        List<AdminRoleCode> activeRoles,
        LocalDateTime lastLoginAt
    ) {
        return new AdminAccountResponse(
            admin.getAdminId().toString(),
            admin.getAdminCode(),
            admin.getAdminName(),
            activeRoles,
            admin.getCreatedAt(),
            lastLoginAt
        );
    }
}
