package com.zipdamember.domain.verification.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.zipdamember.domain.verification.constant.EmailVerificationPurposePolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "email_verification")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {
    @Id
    @Column(name = "verification_id", columnDefinition = "BIGINT UNSIGNED", nullable = false)
    private Long verificationId;

    @Column(name = "verification_email", length = 64, nullable = false)
    private String verificationEmail;

    @Column(name = "purpose", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private EmailVerificationPurposePolicy purpose;

    @Column(name = "verification_code", length = 64, nullable = false)
    private String verificationCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "attempt_count", columnDefinition = "INT UNSIGNED", nullable = false)
    private int attemptCount = 0;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private EmailVerification(
            String verificationEmail,
            EmailVerificationPurposePolicy purpose,
            String verificationCode,
            LocalDateTime expiresAt
    ) {
        this.verificationEmail = verificationEmail;
        this.purpose = purpose;
        this.verificationCode = verificationCode;
        this.expiresAt = expiresAt;
        this.attemptCount = 0;
    }

    public static EmailVerification create(
            String verificationEmail,
            EmailVerificationPurposePolicy purpose,
            String verificationCode,
            LocalDateTime expiresAt
    ) {
        return new EmailVerification(
                verificationEmail,
                purpose,
                verificationCode,
                expiresAt
        );
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean hasReachedAttemptLimit(int maximumAttempts) {
        return attemptCount >= maximumAttempts;
    }

    public void increaseAttemptCount() {
        attemptCount++;
    }

    public void verify(LocalDateTime now) {
        verifiedAt = now;
    }

    @PrePersist
    private void generateVerificationId() {
        if (verificationId == null) {
            verificationId = TsidCreator.getTsid().toLong();
        }
    }
}
