package com.zipdamember.global.error.custom.business;


import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;

public class NotFoundResourceException extends BusinessException {
    public NotFoundResourceException(String message) {
        super(CustomResponseCode.NOT_REGISTERED_ERROR, message);
    }
}
