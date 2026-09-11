package com.zipdamember.domain.member.repository;

import com.zipdamember.domain.member.entity.MemberSanction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface MemberSanctionRepository extends JpaRepository<MemberSanction, Long> {

    // @Lock(PESSIMISTIC_WRITE): DB가 조회 제재 행에 쓰기 잠금을 획득하여 동시 해제 요청을 직렬화
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MemberSanction> findWithLockBySanctionId(Long sanctionId);
}
