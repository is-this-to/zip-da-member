package com.zipdamember.domain.member.request;

import com.zipdamember.domain.member.constant.ProfileImageUpdatePolicy;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberProfileUpdateRequest(
        @Size(min = 2, max = 10)
        @Pattern(regexp = "^[가-힣a-zA-Z0-9_]+$")
        String nickname,
        @Pattern(regexp = "^01[016789]\\d{7,8}$")
        String phone,
        ProfileImageUpdatePolicy profileImageAction,
        String profileFileId
) {
}
