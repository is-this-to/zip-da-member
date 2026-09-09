package com.zipdamember.domain.member.repository;

import com.querydsl.core.types.Predicate;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.global.jpa.JPAWithDeleted;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.Repository;

public interface AdminMemberRepository extends Repository<MemberAccount, Long>,
        QuerydslPredicateExecutor<MemberAccount> {
    // 목록·건수 조회시 soft delete된 삭제 데이터 포함
    @JPAWithDeleted
    @Override
    Page<MemberAccount> findAll(
            Predicate predicate, // 동적 검색조건
            Pageable pageable    // 페이징·정렬
    );
}
