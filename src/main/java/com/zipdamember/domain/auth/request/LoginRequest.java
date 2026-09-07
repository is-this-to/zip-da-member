package com.zipdamember.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "로그인 시 필요 데이터")
public record LoginRequest(
    @Schema(description = "이메일", examples = "test@test.com", nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일은 필수 항목입니다")
    @Pattern(regexp = "^[0-9a-zA-Z](?!.*?[\\-_.]{2})[a-zA-Z0-9\\-_.]{3,63}@[0-9a-zA-Z](?!.*?[\\-_.]{2})[a-zA-Z0-9\\-_.]{3,63}\\.[a-zA-Z]{2,3}$", message = "허용하지 않는 양식입니다.")
    String email,

    @Schema(description = "비밀번호", examples = "qwer1234", nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수 항목입니다")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()])[A-Za-z\\d!@#$%^&*()]{8,20}$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8~20자로 입력해주세요."
    )
    String password
) {}
