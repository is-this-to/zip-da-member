package com.zipdamember.domain.verification.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdamember.domain.verification.entity.EmailVerification;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.zipdamember.domain.verification.entity.QEmailVerification.emailVerification;

@RequiredArgsConstructor
public class VerificationEmailRepositoryCustomImpl
        implements VerificationEmailRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<EmailVerification> findByIdForUpdate(
            Long verificationId
    ) {
        EmailVerification result = queryFactory
                .selectFrom(emailVerification)
                .where(
                        emailVerification.verificationId.eq(
                                verificationId
                        )
                )
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public long deleteExpiredBefore(LocalDateTime cutoff) {
        return queryFactory
                .delete(emailVerification)
                .where(emailVerification.expiresAt.lt(cutoff))
                .execute();
    }
}
