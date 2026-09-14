package com.zipdamember.domain.member.repository;

import com.zipdamember.domain.member.entity.MemberAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface MemberAccountRepository extends JpaRepository<MemberAccount, Long> {

    Optional<MemberAccount> findByEmail(String email);

    // @Lock(PESSIMISTIC_WRITE): DB가 조회 회원 행에 쓰기 잠금을 획득하여 동시 제재 요청을 직렬화
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MemberAccount> findWithLockByMemberId(Long memberId);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
