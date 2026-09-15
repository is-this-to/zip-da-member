package com.zipdamember.global.jwt.request;

import com.zipdamember.domain.admin.constant.AdminRoleCode;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public record AdminTokenGenerateRequest(
        Long adminId,
        Collection<AdminRoleCode> roles
) {
    public AdminTokenGenerateRequest {
        if (adminId == null) {
            throw new NullPointerException("관리자 ID는 필수입니다.");
        }

        if (roles == null) {
            throw new NullPointerException("관리자 역할은 필수입니다.");
        }

        if (adminId <= 0) {
            throw new IllegalArgumentException("관리자 ID는 양수여야 합니다.");
        }

        roles = List.copyOf(new LinkedHashSet<>(roles));
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("관리자 역할은 하나 이상이어야 합니다.");
        }
    }
}
