package com.zipdamember.domain.member.event;

public record MemberProfileUpdatedEvent(
        String memberId,
        String nickname,
        String profileFileId
) {
}
