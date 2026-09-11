package com.zipdamember.domain.member.response;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.member.constant.MemberSanctionScope;
import com.zipdamember.domain.member.constant.MemberStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminMemberSanctionHistoryResponse(
        List<History> histories
) {
    public static AdminMemberSanctionHistoryResponse from(List<History> histories) {
        return new AdminMemberSanctionHistoryResponse(List.copyOf(histories));
    }

    public record History(
            Long sanctionId,
            String historyType,
            MemberStatus beforeStatus,
            MemberStatus afterStatus,
            MemberSanctionScope scope,
            String reason,
            AdminRoleCode changedByRoleCode,
            String changedByName,
            LocalDateTime changedAt
    ) {
    }
}
