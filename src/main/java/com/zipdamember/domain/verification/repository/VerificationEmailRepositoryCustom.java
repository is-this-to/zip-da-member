package com.zipdamember.domain.verification.repository;

import com.zipdamember.domain.verification.entity.EmailVerification;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationEmailRepositoryCustom {

    Optional<EmailVerification> findByIdForUpdate(
            Long verificationId
    );

    long deleteExpiredBefore(LocalDateTime cutoff);
}
