package com.zipdamember.domain.member.request;

import com.zipdamember.domain.member.constant.MemberSanctionScope;
import com.zipdamember.domain.member.constant.MemberSuspensionReasonCode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record AdminMemberSuspendRequest(
        @NotNull(message = "제재 범위는 필수입니다.")
        MemberSanctionScope scope,

        @NotNull(message = "제재 사유 코드는 필수입니다.")
        MemberSuspensionReasonCode reasonCode,

        @Positive(message = "연관 신고 아이디는 양수여야 합니다.")
        Long relatedReportId,

        @NotNull(message = "제재 시작일시는 필수입니다.")
        LocalDateTime startAt,

        LocalDateTime endAt
) {
    @AssertTrue(message = "제재 종료일시는 시작일시보다 이후여야 합니다.")
    public boolean isEndAtAfterStartAt() {
        return endAt == null || startAt == null || endAt.isAfter(startAt);
    }
}
