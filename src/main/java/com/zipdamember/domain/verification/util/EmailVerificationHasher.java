package com.zipdamember.domain.verification.util;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class EmailVerificationHasher {

    private final PasswordEncoder passwordEncoder;

    /**
     * 이메일 SHA-256 암호화 해주는 메서드
     * @param email 암호화할 평문 이메일
     * @return 암호화 된 이메일
     */
    public String hashEmail(String email) {
        try {
            // MessageDigest: 자바에서 해시 알고리즘을 실행해 주는 객체, getInstance("SHA-256"): SHA-256 알고리즘을 사용하는 MessageDigest 객체 생성
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            //email.getBytes(StandardCharsets.UTF_8): 자바 문자열을 바이트 배열로 변경
            //messageDigest.digest(...): 전달 받은 바이트 배열을 해싱하고 byte[]로 반환
            byte[] digest = messageDigest.digest(email.getBytes(StandardCharsets.UTF_8));

            // 32바이트 글자를 저장 가능한 64자리 문자열로 변환함
            return HexFormat.of().formatHex(digest);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("이메일 해시 생성에 실패했습니다.", exception);
        }
    }

    public String hashVerificationCode(String verificationCode) {
        return passwordEncoder.encode(verificationCode);
    }

    public boolean matches(
            String verificationCode,
            String verificationCodeHash
    ) {
        return passwordEncoder.matches(
                verificationCode,
                verificationCodeHash
        );
    }
}
