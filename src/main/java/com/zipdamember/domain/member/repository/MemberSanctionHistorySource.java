package com.zipdamember.domain.member.repository;

import com.zipdamember.domain.member.constant.MemberSanctionScope;
import com.zipdamember.domain.member.constant.MemberSuspensionReasonCode;

import java.time.LocalDateTime;

public record MemberSanctionHistorySource(
        Long sanctionId,
        MemberSanctionScope scope,
        MemberSuspensionReasonCode reasonCode,
        LocalDateTime startAt,
        LocalDateTime releasedAt,
        String sanctionedByName,
        String releasedByName
) {
}
