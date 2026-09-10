package com.zipdamember.domain.auth.response;

import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.global.security.constant.MemberRolePolicy;

import java.time.LocalDateTime;

public record CreateMemberResponse(
        String memberId,
        String email,
        String nickname,
        MemberRolePolicy role,
        MemberStatus status,
        LocalDateTime createdAt
) {
    public static CreateMemberResponse from(MemberAccount memberAccount) {
        return new CreateMemberResponse(
                memberAccount.getMemberId().toString(),
                memberAccount.getEmail(),
                memberAccount.getNickname(),
                memberAccount.getMemberRole(),
                memberAccount.getStatus(),
                memberAccount.getCreatedAt()
        );
    }
}
