package com.zipdamember.domain.verification.response;

import com.zipdamember.domain.verification.entity.EmailVerification;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 인증번호 발송 응답")
public record EmailVerificationResponse(

        @Schema(description = "이메일 인증 식별자", type = "string")
        String verificationId,

        @Schema(description = "인증번호 만료 시간(초)", example = "300")
        int expiresInSeconds,

        @Schema(description = "재발송 가능 시간(초)", example = "60")
        int resendAfterSeconds
) {

    public static EmailVerificationResponse from(
            EmailVerification emailVerification,
            int expiresInSeconds,
            int resendAfterSeconds
    ) {
        return new EmailVerificationResponse(
                emailVerification.getVerificationId().toString(),
                expiresInSeconds,
                resendAfterSeconds
        );
    }
}
