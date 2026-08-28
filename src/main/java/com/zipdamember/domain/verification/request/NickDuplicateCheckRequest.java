package com.zipdamember.domain.verification.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "닉네임 가입 유효성 검사")
public record NickDuplicateCheckRequest(
        @Schema(description = "닉네임", example = "기분이좋음", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 50, message = "닉네임은 50자 이하로 입력해야 합니다.")
        String nickName
) {
}
