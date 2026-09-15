package com.zipdamember.global.error.custom.business;

import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;

public class OcrProcessingException extends BusinessException {

    public OcrProcessingException(String message) {
        super(CustomResponseCode.OCR_PROCESSING_ERROR, message);
    }

    public OcrProcessingException(String message, Throwable cause) {
        super(CustomResponseCode.OCR_PROCESSING_ERROR, message, cause);
    }
}
