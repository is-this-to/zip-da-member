package com.zipdamember.global.error.custom.business;

import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;

public class DuplicatedRecordException extends BusinessException {
    public DuplicatedRecordException(String message) {
        super(CustomResponseCode.NOT_FOUND_RESOURCE_ERROR, message);
    }
}
