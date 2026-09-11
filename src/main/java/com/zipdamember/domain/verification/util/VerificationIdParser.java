package com.zipdamember.domain.verification.util;

import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;

public final class VerificationIdParser {

    private VerificationIdParser() {
    }

    public static Long parse(String verificationId) {
        if (verificationId == null || !verificationId.matches("^[1-9]\\d*$")) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "올바르지 않은 이메일 인증 식별자입니다."
            );
        }

        try {
            return Long.parseLong(verificationId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "올바르지 않은 이메일 인증 식별자입니다."
            );
        }
    }
}
