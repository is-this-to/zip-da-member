package com.zipdamember.domain.auth.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "password_reset_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {
    @Id
    @Column(name = "reset_id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long resetId;
    @Column(name = "member_id", columnDefinition = "BIGINT UNSIGNED")
    private Long memberId;
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    @Column(name = "password_token", nullable = false, unique = true, length = 512)
    private String passwordToken;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public static PasswordResetToken create(Long memberId, String email, String passwordToken, LocalDateTime expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.memberId = memberId;
        token.email = email;
        token.passwordToken = passwordToken;
        token.expiresAt = expiresAt;
        return token;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void use(LocalDateTime now) {
        if (isUsed()) {
            throw new IllegalStateException("이미 사용한 비밀번호 재설정 토큰입니다.");
        }
        usedAt = now;
    }

    @PrePersist
    private void generateId() {
        if (resetId == null) {
            resetId = TsidCreator.getTsid().toLong();
        }
    }
}
