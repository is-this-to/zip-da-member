package com.zipdamember.domain.admin.request;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAccountCreateRequest(
        @Schema(description = "관리자 로그인 코드", example = "sales-admin-01")
        @NotBlank
        @Size(max = 20)
        String adminCode,

        @Schema(description = "관리자 성명", example = "홍길동")
        @NotBlank
        @Size(max = 50)
        String adminName,

        @Schema(description = "최초 부여 관리자 역할", example = "SALES_ADMIN")
        @NotNull
        AdminRoleCode adminRole
) {
    public AdminAccountCreateRequest {
        adminCode = adminCode == null ? null : adminCode.strip();
        adminName = adminName == null ? null : adminName.strip();
    }
}
