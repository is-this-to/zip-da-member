package com.zipdamember.domain.auth.repository;

import com.zipdamember.domain.auth.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.List;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByPasswordToken(String passwordToken);

    List<PasswordResetToken> findAllByMemberIdAndUsedAtIsNull(Long memberId);
}
