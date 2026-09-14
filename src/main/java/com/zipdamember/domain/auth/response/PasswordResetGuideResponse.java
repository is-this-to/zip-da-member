package com.zipdamember.domain.auth.response;

public record PasswordResetGuideResponse(String message) {
    public static PasswordResetGuideResponse accepted() {
        return new PasswordResetGuideResponse(
                "입력한 이메일로 비밀번호 재설정 안내를 발송했습니다. 가입 여부와 관계없이 동일하게 안내됩니다."
        );
    }
}
