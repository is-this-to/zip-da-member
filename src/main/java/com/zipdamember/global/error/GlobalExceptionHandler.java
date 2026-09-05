package com.zipdamember.global.error;

import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.GlobalResponseDTO;
import com.zipdamember.global.response.constant.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

import org.springframework.security.access.AccessDeniedException;
import java.util.Map;
import java.util.stream.Collectors;

// 커스텀한 Exception을 위해 만든 객체
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<GlobalResponseDTO<Void>> generateErrorResponse(CustomResponseCode customResponseCode) {
        return ResponseEntity.status(customResponseCode.getHttpStatus())
                .body(GlobalResponseDTO.<Void>from(customResponseCode));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(BusinessException e) {
        log.debug(e.getMessage(), e);
        return this.generateErrorResponse(e.getCustomResponseCode());
    }

    // -----------------------------------------
    // 이하 Spring에서 발생하는 Exception
    // -----------------------------------------
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(AccessDeniedException e) {
        log.debug(CustomResponseCode.UNAUTHORIZED_ERROR.name(), e);
        // 현재 로그인한 사용자의 정보를 컨텍스트에서 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 로그인하지 않은 익명 사용자가 접근한 경우 (인증 실패 - 401)
        if (authentication instanceof AnonymousAuthenticationToken) {
            return this.generateErrorResponse(CustomResponseCode.UNAUTHENTICATED_ERROR); // E02
        }

        // 로그인은 했으나 권한(Role)이 부족한 경우 (인가 실패 - 403)
        return this.generateErrorResponse(CustomResponseCode.UNAUTHORIZED_ERROR);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(MethodArgumentTypeMismatchException e) {
        log.debug(CustomResponseCode.INVALID_PARAMETER_ERROR.name(), String.format("%s : 필드를 확인해 주세요.", e.getName()));
        return this.generateErrorResponse(CustomResponseCode.INVALID_PARAMETER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField, // 필드명
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "유효하지 않은 값입니다.",
                        (existing, replacement) -> existing // 중복 필드가 있을 경우 기존 값 유지
                ));

        log.debug(CustomResponseCode.INVALID_PARAMETER_ERROR.name(), errors);
        return this.generateErrorResponse(CustomResponseCode.INVALID_PARAMETER_ERROR);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(NoResourceFoundException e) {
        log.debug(CustomResponseCode.NOT_FOUND_ERROR.name(), e);
        return this.generateErrorResponse(CustomResponseCode.NOT_FOUND_ERROR);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(DuplicateKeyException e) {
        log.error("DB 에러", e);
        return this.generateErrorResponse(CustomResponseCode.DB_DUPLICATED_KEY_ERROR);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(DataAccessException e) {
        log.error("DB 에러", e);
        return this.generateErrorResponse(CustomResponseCode.DB_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handle(Exception e) {
        log.error("시스템 에러", e);
        return this.generateErrorResponse(CustomResponseCode.SYSTEM_ERROR);
    }
}
