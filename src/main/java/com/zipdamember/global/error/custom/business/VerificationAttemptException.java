package com.zipdamember.global.error.custom.business;

import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;

public class VerificationAttemptException extends BusinessException {

    public VerificationAttemptException(
            CustomResponseCode customResponseCode,
            String message
    ) {
        super(customResponseCode, message);
    }
}
