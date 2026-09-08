package com.zipdamember.domain.verification.validator;

import com.zipdamember.domain.verification.request.ValidateMemberRequest;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class MemberFormatValidator {

    private final Validator validator;

    // 닉네임 정책이 한글·영문·숫자·밑줄, 2~10자일 경우
    private static final Pattern NICKNAME_PATTERN = Pattern.compile(
            "^[가-힣a-zA-Z0-9_]{2,10}$"
    );

    private record EmailValue(
            @NotBlank
            @Email
            String value
    ){}

    public void validate(ValidateMemberRequest request) {

        boolean valid = switch (request.typePolicy()) {
            case EMAIL ->
                    validator.validate(new EmailValue(request.value())).isEmpty();

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
