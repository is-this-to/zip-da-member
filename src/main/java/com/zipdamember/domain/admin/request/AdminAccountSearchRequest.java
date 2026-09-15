package com.zipdamember.domain.admin.request;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminAccountSearchRequest(
    @Schema(description = "관리자 코드 부분일치 검색어", example = "sales")
    @Size(max = 20) String adminCode,

    @Schema(description = "관리자 성명 부분일치 검색어", example = "홍길동")
    @Size(max = 50) String adminName,

    @Schema(description = "관리자 권한 정확 일치 조건", example = "SALES_ADMIN")
    AdminRoleCode role,

    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size
) {
    public AdminAccountSearchRequest {
        adminCode = adminCode == null || adminCode.isBlank() ? null : adminCode.strip();
        adminName = adminName == null || adminName.isBlank() ? null : adminName.strip();
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
