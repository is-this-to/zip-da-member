package com.zipdamember.domain.auth.repository;

import com.zipdamember.domain.auth.entity.AdminLoginSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface AdminLoginSessionRepository extends JpaRepository<AdminLoginSession, Long> {
    // @Lock(PESSIMISTIC_WRITE) : 동일한 세션을 여러 요청이 동시에 재발급하지 못하도록 막음
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AdminLoginSession> findByRefreshToken(String refreshToken);

    long deleteAllByAdminId(Long adminId);

    List<AdminLoginSession> findAllByAdminIdAndRevokedAtIsNull(Long adminId);
}
