package com.zipdamember.global.error.custom.business;


import com.zipdamember.global.response.constant.CustomResponseCode;
import com.zipdamember.global.error.custom.BusinessException;

public class AlreadyRegisteredException extends BusinessException {
    public AlreadyRegisteredException(String message) {
        super(CustomResponseCode.ALREADY_REGISTERED_ERROR, message);
    }
}
