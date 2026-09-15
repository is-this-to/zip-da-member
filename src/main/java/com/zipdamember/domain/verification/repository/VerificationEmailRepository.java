package com.zipdamember.domain.verification.repository;

import com.zipdamember.domain.verification.constant.EmailVerificationPurposePolicy;
import com.zipdamember.domain.verification.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface VerificationEmailRepository
        extends JpaRepository<EmailVerification, Long>,
        VerificationEmailRepositoryCustom {

    boolean existsByVerificationEmailAndPurposeAndCreatedAtAfter(
            String verificationEmail,
            EmailVerificationPurposePolicy purpose,
            LocalDateTime createdAt
    );
}
