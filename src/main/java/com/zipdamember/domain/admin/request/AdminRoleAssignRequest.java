package com.zipdamember.domain.admin.request;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AdminRoleAssignRequest(
        @Schema(description = "부여할 관리자 역할", example = "CS_ADMIN")
        @NotNull
        AdminRoleCode roleCode
) {
}
