package com.zipdamember.domain.verification.request;

import com.zipdamember.domain.verification.constant.MemberValidationTypePolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "중복 여부 검사 시 필요 데이터")
public record ValidateMemberRequest(
        @NotNull
        @Schema(description = "중복 검증 항목", example = "EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
        MemberValidationTypePolicy typePolicy,

        @NotNull
        @Schema(description = "중복 검증 값", example = "jiyoon0114@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String value
) {
}
