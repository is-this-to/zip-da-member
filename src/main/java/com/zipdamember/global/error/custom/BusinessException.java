package com.zipdamember.global.error.custom;

import com.zipdamember.global.response.constant.CustomResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class BusinessException extends RuntimeException {
    private final CustomResponseCode customResponseCode;

    public BusinessException(CustomResponseCode customResponseCode, String message) {
        super(message);
        this.customResponseCode = customResponseCode;
    }

    public BusinessException(
            CustomResponseCode customResponseCode,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.customResponseCode = customResponseCode;
    }
}
