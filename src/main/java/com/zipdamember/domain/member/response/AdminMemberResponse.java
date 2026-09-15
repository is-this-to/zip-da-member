package com.zipdamember.domain.member.response;

import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.global.security.constant.MemberRolePolicy;

import java.time.LocalDateTime;

public record AdminMemberResponse(
        String memberId,
        String name,
        String email,
        String nickname,
        MemberRolePolicy role,
        MemberStatus status,
        LocalDateTime createdAt
) {
    public static AdminMemberResponse from(MemberAccount member) {
        return new AdminMemberResponse(
                member.getMemberId().toString(), maskName(member.getName()), maskEmail(member.getEmail()),
                member.getNickname(), member.getMemberRole(), member.getStatus(), member.getCreatedAt()
        );
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "*";
        }
        int length = name.codePointCount(0, name.length());
        if (length == 1) {
            return "*";
        }
        int firstEnd = name.offsetByCodePoints(0, 1);
        if (length == 2) {
            return name.substring(0, firstEnd) + "*";
        }
        int lastStart = name.offsetByCodePoints(0, length - 1);
        return name.substring(0, firstEnd) + "*".repeat(length - 2) + name.substring(lastStart);
    }

    private static String maskEmail(String email) {
        if (email == null) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at < 1) {
            return "***";
        }
        // 짧은 로컬 파트도 최소 한 글자는 숨긴다.
        int visible = Math.min(2, at - 1);
        return email.substring(0, visible) + "***" + email.substring(at);
    }
}
