package com.zipdamember.domain.member.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminMemberSuspendReleaseRequest(
        @NotBlank(message = "정지 해제 사유는 필수입니다.")
        @Size(max = 500, message = "정지 해제 사유는 500자를 초과할 수 없습니다.")
        String releaseReason
) {
}
