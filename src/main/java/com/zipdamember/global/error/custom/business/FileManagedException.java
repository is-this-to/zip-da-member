package com.zipdamember.global.error.custom.business;

import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;

public class FileManagedException extends BusinessException {
    public FileManagedException(String message) {
        super(CustomResponseCode.FILE_MANAGED_ERROR, message);
    }
}
