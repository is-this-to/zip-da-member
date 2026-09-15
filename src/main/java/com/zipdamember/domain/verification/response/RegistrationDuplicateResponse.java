package com.zipdamember.domain.verification.response;

import com.zipdamember.domain.verification.constant.MemberValidationTypePolicy;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 중복 검사 답변")
public record RegistrationDuplicateResponse(
        @Schema(description = "사용 가능 여부")
        boolean available,
        @Schema(description = "검사한 항목")
        MemberValidationTypePolicy field
) {
        public static RegistrationDuplicateResponse from(boolean available, MemberValidationTypePolicy field) {
                return new RegistrationDuplicateResponse(available, field);
        }
}
