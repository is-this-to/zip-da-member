package com.zipdamember.domain.admin.response;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.admin.entity.Admin;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminAccountCreateResponse(
        @Schema(description = "생성된 관리자 식별자", example = "471234567890123481")
        String adminId,

        @Schema(description = "생성된 관리자 로그인 코드", example = "sales-admin-01")
        String adminCode,

        @Schema(description = "생성된 관리자 성명", example = "홍길동")
        String adminName,

        @Schema(description = "최초 부여 관리자 역할", example = "SALES_ADMIN")
        AdminRoleCode adminRole,

        @Schema(description = "최초 로그인 비밀번호 변경 필요 여부", example = "true")
        boolean passwordChangeRequired,

        @Schema(description = "신규 관리자에게 1회만 전달할 초기 비밀번호. 로그·감사 로그에 저장하지 않음")
        String initialPassword
) {
    public static AdminAccountCreateResponse of(
            Admin admin,
            AdminRoleCode adminRole,
            String initialPassword
    ) {
        return new AdminAccountCreateResponse(
                admin.getAdminId().toString(),
                admin.getAdminCode(),
                admin.getAdminName(),
                adminRole,
                Boolean.TRUE.equals(admin.getPasswordChangeRequired()),
                initialPassword
        );
    }
}
