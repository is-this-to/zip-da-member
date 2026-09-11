package com.zipdamember.domain.auth.response;

import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.global.security.constant.MemberRolePolicy;
import io.swagger.v3.oas.annotations.media.Schema;

public record MemberPrincipalResponse(
        @Schema(description = "회원 식별자", example = "123")
        String userId,

        @Schema(description = "이메일", example = "user@zipda.com")
        String email,

        @Schema(description = "성명", example = "홍길동")
        String name,

        @Schema(description = "닉네임", example = "집다회원")
        String nick,

        @Schema(description = "휴대전화 번호", example = "01012345678")
        String phone,

        @Schema(description = "회원 역할", example = "USER")
        MemberRolePolicy role
) {
    public static MemberPrincipalResponse from(MemberAccount member) {
        return new MemberPrincipalResponse(
                member.getMemberId().toString(),
                member.getEmail(),
                member.getName(),
                member.getNickname(),
                member.getPhone(),
                member.getMemberRole()
        );
    }
}
