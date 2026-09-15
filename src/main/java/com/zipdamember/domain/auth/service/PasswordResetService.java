package com.zipdamember.domain.auth.service;

import com.zipdamember.domain.auth.entity.PasswordResetToken;
import com.zipdamember.domain.auth.repository.LoginSessionRepository;
import com.zipdamember.domain.auth.repository.PasswordResetTokenRepository;
import com.zipdamember.domain.auth.request.PasswordResetEmailRequest;
import com.zipdamember.domain.auth.request.PasswordResetRequest;
import com.zipdamember.domain.auth.response.PasswordResetGuideResponse;
import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.global.config.auth.PasswordResetConfig;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.mail.EmailSender;
import com.zipdamember.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final int TOKEN_BYTES = 32;

    private final MemberAccountRepository memberAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetConfig passwordResetConfig;
    private final EmailSender emailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public PasswordResetGuideResponse requestReset(PasswordResetEmailRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Optional<MemberAccount> member = memberAccountRepository.findByEmail(email)
                .filter(value -> value.getStatus() == MemberStatus.ACTIVE)
                .filter(value -> value.getWithdrawnAt() == null);

        if (member.isEmpty()) {
            return PasswordResetGuideResponse.accepted();
        }

        String rawToken = createRawToken();
        LocalDateTime now = LocalDateTime.now();
        passwordResetTokenRepository
                .findAllByMemberIdAndUsedAtIsNull(member.get().getMemberId())
                .forEach(token -> token.use(now));
        PasswordResetToken resetToken = PasswordResetToken.create(
                member.get().getMemberId(),
                email,
                hash(rawToken),
                now.plusMinutes(passwordResetConfig.tokenExpiryMinutes())
        );
        passwordResetTokenRepository.saveAndFlush(resetToken);

        String resetLink = UriComponentsBuilder
                .fromUriString(passwordResetConfig.frontendResetUri())
                .queryParam("token", rawToken)
                .build()
                .toUriString();
        try {
            emailSender.sendPasswordResetLink(email, resetLink);
        } catch (RuntimeException exception) {
            passwordResetTokenRepository.delete(resetToken);
            log.error("비밀번호 재설정 메일 발송 실패: memberId={}", member.get().getMemberId(), exception);
        }
        return PasswordResetGuideResponse.accepted();
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        validatePasswordConfirmation(request.newPassword(), request.newPasswordCheck());
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByPasswordToken(hash(request.token()))
                .orElseThrow(() -> new BusinessException(
                        CustomResponseCode.PASSWORD_RESET_TOKEN_INVALID,
                        "비밀번호 재설정 링크가 올바르지 않거나 이미 사용되었습니다."
                ));

        if (resetToken.isUsed()) {
            throw new BusinessException(
                    CustomResponseCode.PASSWORD_RESET_TOKEN_INVALID,
                    "비밀번호 재설정 링크가 올바르지 않거나 이미 사용되었습니다."
            );
        }
        if (resetToken.isExpired(now)) {
            throw new BusinessException(
                    CustomResponseCode.PASSWORD_RESET_TOKEN_EXPIRED,
                    "비밀번호 재설정 링크가 만료되었습니다."
            );
        }

        MemberAccount member = memberAccountRepository.findById(resetToken.getMemberId())
                .filter(value -> value.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        CustomResponseCode.PASSWORD_RESET_TOKEN_INVALID,
                        "비밀번호를 재설정할 수 없는 계정입니다."
                ));
        member.changePassword(passwordEncoder.encode(request.newPassword()));
        passwordResetTokenRepository
                .findAllByMemberIdAndUsedAtIsNull(member.getMemberId())
                .forEach(token -> token.use(now));
        revokeAllSessions(member.getMemberId());
    }

    private void revokeAllSessions(Long memberId) {
        loginSessionRepository.findAllByMemberIdAndRevokedAtIsNull(memberId)
                .forEach(session -> session.revoke());
    }

    private void validatePasswordConfirmation(String password, String passwordCheck) {
        if (!password.equals(passwordCheck)) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }
    }

    private String createRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("비밀번호 재설정 토큰 해시에 실패했습니다.", exception);
        }
    }
}
