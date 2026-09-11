package com.zipdamember.global.response.constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CustomResponseCode {
    SUCCESS(HttpStatus.OK, "00")
    // 인증 관련 에러
    ,NOT_REGISTERED_ERROR(HttpStatus.UNAUTHORIZED, "E01")
    , ALREADY_REGISTERED_ERROR(HttpStatus.CONFLICT, "E02")
    , UNAUTHENTICATED_ERROR(HttpStatus.UNAUTHORIZED, "E3")
    , UNAUTHORIZED_ERROR(HttpStatus.FORBIDDEN, "E04")
    , INVALID_TOKEN_ERROR(HttpStatus.UNAUTHORIZED, "E05")
    // Not Found Resource 관련 에러
    , NOT_FOUND_RESOURCE_ERROR(HttpStatus.NOT_FOUND, "E10")
    , DUPLICATED_RESOURCE_ERROR(HttpStatus.CONFLICT, "E11")
    // 유효성 관련 에러
    , INVALID_PARAMETER_ERROR(HttpStatus.BAD_REQUEST, "E21")
    // OAuth2 관련 에러
    , OAUTH2_ERROR(HttpStatus.CONFLICT, "E30")
    , UNSUPPORTED_PROVIDER_ERROR(HttpStatus.CONFLICT, "E31")
    // File 관련
    , FILE_MANAGED_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E40")
    , OCR_PROCESSING_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "E41")
    // 중개사 전환 신청 관련
    , AGENT_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "E42")
    , AGENT_APPLICATION_NOT_EDITABLE(HttpStatus.CONFLICT, "E43")
    , AGENT_APPLICATION_DUPLICATED(HttpStatus.CONFLICT, "E44")
    // Not Found 관련
    , NOT_FOUND_ERROR(HttpStatus.NOT_FOUND, "E50")
    // Email 관련
    , EMAIL_SEND_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "E60")
    , EMAIL_RESEND_LIMIT_ERROR(HttpStatus.TOO_MANY_REQUESTS, "E61")
    , EMAIL_VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "E62")
    , EMAIL_VERIFICATION_EXPIRED(HttpStatus.GONE, "E63")
    , EMAIL_VERIFICATION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "E64")
    , VERIFICATION_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "E65")
    , VERIFICATION_CODE_INVALID(HttpStatus.BAD_REQUEST, "E66")
    , EMAIL_VERIFICATION_EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "E67")
    , EMAIL_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "E68")
    , PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "E69")
    , PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.GONE, "E70")
    // DB 관련
    , DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E80")
    , DB_DUPLICATED_KEY_ERROR(HttpStatus.CONFLICT, "E81")
    // 시스템 에러
    , SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E99")
    ;

    private final HttpStatus httpStatus;
    private final String code;

    CustomResponseCode(HttpStatus httpStatus, String code) {
        this.httpStatus = httpStatus;
        this.code = code;
    }
}
