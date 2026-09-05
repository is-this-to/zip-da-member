package com.zipdamember.domain.auth.repository;

import com.zipdamember.domain.auth.entity.LoginSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

public interface LoginSessionRepository extends JpaRepository<LoginSession, Long> {
    // Service의 같은 트랜잭션 안에서 조회부터 토큰 교체까지 수행한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LoginSession> findByRefreshToken(String refreshToken);
}
