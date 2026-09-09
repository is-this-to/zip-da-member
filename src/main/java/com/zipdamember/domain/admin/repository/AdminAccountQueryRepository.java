package com.zipdamember.domain.admin.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdamember.domain.admin.constant.AdminLoginResult;
import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.admin.entity.Admin;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.zipdamember.domain.admin.entity.QAdmin.admin;
import static com.zipdamember.domain.admin.entity.QAdminLoginHistory.adminLoginHistory;
import static com.zipdamember.domain.admin.entity.QAdminRoleAssignment.adminRoleAssignment;

@Repository
public class AdminAccountQueryRepository {
    private final JPAQueryFactory queryFactory;

    public AdminAccountQueryRepository(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    public Page<Admin> searchAdmins(String pattern, AdminRoleCode role, Pageable pageable) {
        // 관리자 목록 조회
        List<Admin> content = queryFactory
            .selectFrom(admin)
            .where(keywordContains(pattern), roleExists(role))
            .orderBy(admin.createdAt.desc(), admin.adminId.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        // 관리자 개수 조회
        var countQuery = queryFactory
            .select(admin.count())
            .from(admin)
            .where(keywordContains(pattern), roleExists(role));

        return PageableExecutionUtils.getPage(
            content,
            pageable,
            () -> Optional.ofNullable(countQuery.fetchOne()).orElse(0L)
        );
    }

    public List<AdminLastLogin> findLastSuccessfulLogins(Collection<Long> adminIds) {
        // 최근 성공 로그인 집계
        var lastLoginAt = adminLoginHistory.occurredAt.max();
        return queryFactory
            .select(adminLoginHistory.adminId, lastLoginAt)
            .from(adminLoginHistory)
            .where(
                adminLoginHistory.adminId.in(adminIds),
                adminLoginHistory.result.eq(AdminLoginResult.SUCCESS)
            )
            .groupBy(adminLoginHistory.adminId)
            .fetch()
            .stream()
            .map(tuple -> new AdminLastLogin(
                tuple.get(adminLoginHistory.adminId),
                tuple.get(lastLoginAt)
            ))
            .toList();
    }

    private BooleanExpression keywordContains(String pattern) {
        if (pattern == null) {
            return null;
        }
        return admin.adminCode.like(pattern, '!')
            .or(admin.adminName.like(pattern, '!'));
    }

    private BooleanExpression roleExists(AdminRoleCode role) {
        if (role == null) {
            return null;
        }
        return JPAExpressions
            .selectOne()
            .from(adminRoleAssignment)
            .where(
                adminRoleAssignment.adminId.eq(admin.adminId),
                adminRoleAssignment.roleCode.eq(role)
            )
            .exists();
    }

    public record AdminLastLogin(Long adminId, LocalDateTime lastLoginAt) {
    }
}
