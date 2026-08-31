package com.zipdamember.domain.admin.entity;

import com.zipdamember.global.jpa.tsid.TsidGenerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "admin_login_session")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminLoginSession {

    @Id
    @TsidGenerated
    @Column(
            name = "session_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BIGINT UNSIGNED"
    )
    private Long sessionId;

    @Column(
            name = "admin_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BIGINT UNSIGNED"
    )
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
}
