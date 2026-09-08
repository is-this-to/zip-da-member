package com.zipdamember.domain.verification.validator;

import com.zipdamember.domain.verification.request.ValidateMemberRequest;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class MemberFormatValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[0-9a-zA-Z](?!.*?[-_.]{2})"
                    + "[a-zA-Z0-9._-]{3,63}"
                    + "@"
                    + "[0-9a-zA-Z](?!.*?[-_.]{2})"
                    + "[a-zA-Z0-9._-]{3,63}"
                    + "\\.[a-zA-Z]{2,3}$"
    );

    // 닉네임 정책이 한글·영문·숫자·밑줄, 2~10자일 경우
    private static final Pattern NICKNAME_PATTERN = Pattern.compile(
            "^[가-힣a-zA-Z0-9_]{2,10}$"
    );

    public void validate(ValidateMemberRequest request) {
        if (request == null || request.typePolicy() == null || request.value() == null) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR
            );
        }

        boolean valid = switch (request.typePolicy()) {
            case EMAIL ->
                    EMAIL_PATTERN.matcher(request.value()).matches();

            case NICKNAME ->
                    NICKNAME_PATTERN.matcher(request.value()).matches();
        };

        if (!valid) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR
            );
        }
    }
}
