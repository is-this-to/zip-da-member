package com.zipdamember.domain.auth.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "admin_login_session")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminLoginSession {
    @Id
    @Column(name = "session_id", nullable = false, updatable = false,columnDefinition = "BIGINT UNSIGNED")
    private Long sessionId;

    @Column(name = "admin_id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long adminId;

    @Column(name = "refresh_token", nullable = false, unique = true, length = 300)
    private String refreshToken;

    @Column(name = "device_id", length = 26)
    private String deviceId;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip", length = 20)
    private String ip;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public static AdminLoginSession create(
            Long adminId,
            String refreshToken,
            String deviceId,
            String userAgent,
            String ip,
            LocalDateTime expiresAt
    ) {
        if (adminId == null || adminId <= 0) {
            throw new IllegalArgumentException("관리자 ID는 양수여야 합니다.");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh Token은 필수입니다.");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("세션 만료일시는 필수입니다.");
        }

        AdminLoginSession session = new AdminLoginSession();
        session.adminId = adminId;
        session.refreshToken = refreshToken;
        session.deviceId = deviceId;
        session.userAgent = userAgent;
        session.ip = ip;
        session.expiresAt = expiresAt;
        return session;
    }

    public void rotate(String refreshToken, LocalDateTime expiresAt) {
        if (isRevoked()) {
            throw new IllegalStateException("폐기된 관리자 로그인 세션입니다.");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh Token은 필수입니다.");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("세션 만료일시는 필수입니다.");
        }

        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }

    public void revoke() {
        if (isRevoked()) {
            throw new IllegalStateException("이미 폐기된 관리자 로그인 세션입니다.");
        }

        this.revokedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return !expiresAt.isAfter(LocalDateTime.now());
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    @PrePersist
    private void generateSessionId() {
        if (sessionId == null) {
            sessionId = TsidCreator.getTsid().toLong();
        }
    }
}
