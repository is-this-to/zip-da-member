package com.zipdamember.domain.admin.request;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminAccountSearchRequest(
    @Size(max = 50) String keyword,
    AdminRoleCode role,
    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size
) {
    public AdminAccountSearchRequest {
        keyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
