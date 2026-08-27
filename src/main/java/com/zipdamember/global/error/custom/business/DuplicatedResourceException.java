package com.zipdamember.global.error.custom.business;


import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;

public class DuplicatedResourceException extends BusinessException {
    public DuplicatedResourceException(String message) {
        super(CustomResponseCode.DUPLICATED_RESOURCE_ERROR, message);
    }
}
