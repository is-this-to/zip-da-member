package com.zipdamember.domain.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "회원가입 필요 양식")
public record CreateMemberRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,

        @NotBlank(message = "이메일 인증 식별자는 필수입니다.")
        @Pattern(regexp = "^[1-9]\\d*$", message = "이메일 인증 식별자는 양수여야 합니다.")
        String verificationId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$])[0-9a-zA-Z!@#$]{8,20}$",
                message = "비밀번호는 영문, 숫자, !@#$ 특수문자를 각각 포함한 8~20자여야 합니다."
        )
        String password,

        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$])[0-9a-zA-Z!@#$]{8,20}$",
                message = "비밀번호는 영문, 숫자, !@#$ 특수문자를 각각 포함한 8~20자여야 합니다."
        )
        String passwordCheck,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 50, message = "이름은 2~50자여야 합니다.")
        String name,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9_]+${2,10}", message = "닉네임은 한글, 영문, 숫자, 밑줄만 사용할 수 있습니다.")
        String nickname,

        @NotBlank(message = "휴대전화 번호는 필수입니다.")
        @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 휴대전화 번호 형식이 아닙니다.")
        String phone,

        @Pattern(regexp = "^[1-9]\\d*$", message = "프로필 파일 식별자는 양수여야 합니다.")
        String profileFileId,

        @NotEmpty(message = "약관 동의 내역은 필수입니다.")
        List<@Valid TermAgreementRequest> termsAgreements
) {
}
