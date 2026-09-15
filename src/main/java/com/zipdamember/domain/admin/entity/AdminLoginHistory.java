package com.zipdamember.domain.admin.entity;

import com.zipdamember.domain.admin.constant.AdminLoginResult;
import com.zipdamember.domain.admin.constant.AdminLoginType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "admin_login_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AdminLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
            name = "admin_login_history_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BIGINT UNSIGNED"
    )
    private Long adminLoginHistoryId;

    @Column(name = "admin_id", updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long adminId;

    @Column(name = "login_identifier_hash", length = 64)
    private String loginIdentifierHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", length = 20)
    private AdminLoginType loginType = AdminLoginType.LOCAL;

    @Column(name = "mfa_used", nullable = false)
    private boolean mfaUsed;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private AdminLoginResult result;

    @Column(name = "failure_reason_code", length = 50)
    private String failureReasonCode;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @CreatedDate
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;
}
