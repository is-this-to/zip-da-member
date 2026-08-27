package com.zipdamember.global.error.custom.business;

import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;

// 커스텀 에러는 보통 RuntimeException을 상속 받음
public class NotRegisteredException extends BusinessException {
    public NotRegisteredException(String message) {
        super(CustomResponseCode.NOT_REGISTERED_ERROR, message);
    }
}
