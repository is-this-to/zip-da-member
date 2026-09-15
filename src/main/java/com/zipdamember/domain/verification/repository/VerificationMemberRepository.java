package com.zipdamember.domain.verification.repository;

import com.zipdamember.domain.member.entity.MemberAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationMemberRepository extends JpaRepository<MemberAccount, Long> {
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickName);
}
