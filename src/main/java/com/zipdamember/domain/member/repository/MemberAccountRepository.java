package com.zipdamember.domain.member.repository;

import com.zipdamember.domain.member.entity.MemberAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberAccountRepository extends JpaRepository<MemberAccount, Long> {

    Optional<MemberAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
