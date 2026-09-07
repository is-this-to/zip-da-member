package com.zipdamember.domain.member.request;

import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.global.security.constant.MemberRolePolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminMemberSearchRequest(
        @Size(max = 255) String keyword,
        MemberStatus status,
        MemberRolePolicy role,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size
) {
    public AdminMemberSearchRequest {
        keyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
