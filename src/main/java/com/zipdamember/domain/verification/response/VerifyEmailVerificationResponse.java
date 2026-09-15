package com.zipdamember.domain.verification.response;

import com.zipdamember.domain.verification.entity.EmailVerification;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "이메일 인증번호 확인 응답")
public record VerifyEmailVerificationResponse(

        @Schema(description = "이메일 인증 식별자", type = "string")
        String verificationId,

        @Schema(description = "인증 완료 여부", example = "true")
        boolean verified,

        @Schema(description = "인증 완료 시각")
        LocalDateTime verifiedAt
) {

    public static VerifyEmailVerificationResponse from(
            EmailVerification emailVerification
    ) {
        return new VerifyEmailVerificationResponse(
                emailVerification.getVerificationId().toString(),
                emailVerification.isVerified(),
                emailVerification.getVerifiedAt()
        );
    }
}
