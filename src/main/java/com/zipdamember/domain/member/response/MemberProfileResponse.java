package com.zipdamember.domain.member.response;

import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.global.security.constant.MemberRolePolicy;

import java.time.LocalDateTime;

public record MemberProfileResponse(
        String memberId,
        String email,
        String name,
        String nickname,
        String phone,
        MemberStatus status,
        MemberRolePolicy role,
        String profileFileId,
        String profileImageUrl,
        LocalDateTime emailVerificationAt,
        MemberAgentSummaryResponse agent
) {
    public static MemberProfileResponse from(
            MemberAccount member,
            String profileImageUrl,
            MemberAgentSummaryResponse agent
    ) {
        return new MemberProfileResponse(
                member.getMemberId().toString(),
                member.getEmail(),
                member.getName(),
                member.getNickname(),
                member.getPhone(),
                member.getStatus(),
                member.getMemberRole(),
                member.getProfileFileId() == null ? null : member.getProfileFileId().toString(),
                profileImageUrl,
                member.getEmailVerificationAt(),
                agent
        );
    }
}
