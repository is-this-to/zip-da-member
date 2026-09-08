package com.zipdamember.domain.member.repository;

import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.global.config.jpa.JPAWithDeleted;
import com.zipdamember.global.security.constant.MemberRolePolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AdminMemberRepository extends Repository<MemberAccount, Long> {
    // 서비스 트랜잭션 안에서 목록과 count 쿼리에 동일한 삭제 필터 상태를 적용한다.
    @JPAWithDeleted
    @Query("""
            select m from MemberAccount m
            where (:pattern is null or m.email like :pattern escape '!'
                or m.nickname like :pattern escape '!')
              and (:status is null or m.status = :status)
              and (:role is null or m.memberRole = :role)
            """)
    Page<MemberAccount> findMembers(
            @Param("pattern") String pattern,
            @Param("status") MemberStatus status,
            @Param("role") MemberRolePolicy role,
            Pageable pageable
    );
}
