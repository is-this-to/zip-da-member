package com.zipdamember.domain.verification.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 중복 검사 답변")
public record RegistrationDuplicateResponse(
        @Schema(description = "사용 가능 여부")
        boolean available,
        @Schema(description = "검사한 항목")
        String field,
        @Schema(description = "안내 메시지")
        String message
) { }
