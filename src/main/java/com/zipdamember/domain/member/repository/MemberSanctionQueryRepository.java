package com.zipdamember.domain.member.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.core.types.Projections;
import com.zipdamember.domain.admin.entity.QAdmin;
import com.zipdamember.domain.member.constant.MemberSanctionScope;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.zipdamember.domain.member.entity.QMemberSanction.memberSanction;

@Repository
public class MemberSanctionQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QAdmin sanctionedByAdmin = new QAdmin("sanctionedByAdmin");
    private static final QAdmin releasedByAdmin = new QAdmin("releasedByAdmin");

    public MemberSanctionQueryRepository(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    public boolean existsActiveByMemberIdAndScope(
            Long memberId,
            MemberSanctionScope scope,
            LocalDateTime baseAt
    ) {
        // 활성 제재 존재 조회
        return queryFactory
                .selectOne()
                .from(memberSanction)
                .where(
                        memberSanction.memberId.eq(memberId),
                        memberSanction.scope.eq(scope),
                        memberSanction.startAt.loe(baseAt),
                        memberSanction.releasedAt.isNull(),
                        memberSanction.endAt.isNull()
                                .or(memberSanction.endAt.gt(baseAt))
                )
                .fetchFirst() != null;
    }

    public boolean existsOtherActiveByMemberIdAndScope(
            Long memberId,
            MemberSanctionScope scope,
            Long excludedSanctionId,
            LocalDateTime baseAt
    ) {
        // 다른 활성 제재 존재 조회
        return queryFactory
                .selectOne()
                .from(memberSanction)
                .where(
                        memberSanction.memberId.eq(memberId),
                        memberSanction.scope.eq(scope),
                        memberSanction.sanctionId.ne(excludedSanctionId),
                        memberSanction.startAt.loe(baseAt),
                        memberSanction.releasedAt.isNull(),
                        memberSanction.endAt.isNull()
                                .or(memberSanction.endAt.gt(baseAt))
                )
                .fetchFirst() != null;
    }

    public List<MemberSanctionHistorySource> findHistorySourcesByMemberId(Long memberId) {
        // 회원 제재·해제 이력 원본 조회
        return queryFactory
                .select(Projections.constructor(
                        MemberSanctionHistorySource.class,
                        memberSanction.sanctionId,
                        memberSanction.scope,
                        memberSanction.reasonCode,
                        memberSanction.startAt,
                        memberSanction.endAt,
                        memberSanction.releasedAt,
                        sanctionedByAdmin.adminName,
                        releasedByAdmin.adminName
                ))
                .from(memberSanction)
                .leftJoin(sanctionedByAdmin)
                .on(sanctionedByAdmin.adminId.eq(memberSanction.adminId))
                .leftJoin(releasedByAdmin)
                .on(releasedByAdmin.adminId.eq(memberSanction.adminReleasedBy))
                .where(memberSanction.memberId.eq(memberId))
                .fetch();
    }
}
