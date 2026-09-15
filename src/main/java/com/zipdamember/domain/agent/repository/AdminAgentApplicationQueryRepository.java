package com.zipdamember.domain.agent.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdamember.domain.agent.constant.AgentApplicationStatus;
import com.zipdamember.domain.agent.entity.QAgencyRegistrationVerification;
import com.zipdamember.domain.agent.entity.QBusinessVerification;
import com.zipdamember.domain.agent.response.AdminAgentApplicationListRow;
import com.zipdamember.domain.member.entity.QMemberAccount;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.zipdamember.domain.agent.entity.QAgentApplication.agentApplication;
import static com.zipdamember.domain.agent.entity.QAgencyRegistrationVerification.agencyRegistrationVerification;
import static com.zipdamember.domain.agent.entity.QBusinessVerification.businessVerification;
import static com.zipdamember.domain.member.entity.QMemberAccount.memberAccount;

@Repository
public class AdminAgentApplicationQueryRepository {

    private final JPAQueryFactory queryFactory;

    public AdminAgentApplicationQueryRepository(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    public Page<AdminAgentApplicationListRow> search(
            String applicant,
            String agencyName,
            String businessRegistrationNo,
            AgentApplicationStatus status,
            Pageable pageable
    ) {
        BooleanBuilder conditions = conditions(
                applicant,
                agencyName,
                businessRegistrationNo,
                status
        );

        QBusinessVerification latestBusinessVerification = new QBusinessVerification("latestBusinessVerification");
        QAgencyRegistrationVerification latestAgencyRegistrationVerification =
                new QAgencyRegistrationVerification("latestAgencyRegistrationVerification");

        List<AdminAgentApplicationListRow> content = queryFactory
                .select(Projections.constructor(
                        AdminAgentApplicationListRow.class,
                        agentApplication.applicationId,
                        memberAccount.name,
                        agentApplication.requestAgencyName,
                        agentApplication.requestBusinessNo,
                        agentApplication.requestAgencyRegistrationNo,
                        agentApplication.status,
                        businessVerification.resultStatus,
                        agencyRegistrationVerification.resultStatus,
                        agentApplication.submittedAt
                ))
                .from(agentApplication)
                .leftJoin(memberAccount)
                .on(memberAccount.memberId.eq(agentApplication.memberId))
                .leftJoin(businessVerification)
                .on(
                        businessVerification.applicationId.eq(agentApplication.applicationId)
                                .and(businessVerification.verificationId.eq(
                                        JPAExpressions.select(latestBusinessVerification.verificationId.max())
                                                .from(latestBusinessVerification)
                                                .where(latestBusinessVerification.applicationId.eq(
                                                        agentApplication.applicationId
                                                ))
                                ))
                )
                .leftJoin(agencyRegistrationVerification)
                .on(
                        agencyRegistrationVerification.applicationId.eq(agentApplication.applicationId)
                                .and(agencyRegistrationVerification.verificationId.eq(
                                        JPAExpressions.select(latestAgencyRegistrationVerification.verificationId.max())
                                                .from(latestAgencyRegistrationVerification)
                                                .where(latestAgencyRegistrationVerification.applicationId.eq(
                                                        agentApplication.applicationId
                                                ))
                                ))
                )
                .where(conditions)
                .orderBy(
                        agentApplication.submittedAt.desc().nullsLast(),
                        agentApplication.applicationId.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(agentApplication.count())
                .from(agentApplication)
                .leftJoin(memberAccount)
                .on(memberAccount.memberId.eq(agentApplication.memberId))
                .where(conditions(
                        applicant,
                        agencyName,
                        businessRegistrationNo,
                        status
                ))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private BooleanBuilder conditions(
            String applicant,
            String agencyName,
            String businessRegistrationNo,
            AgentApplicationStatus status
    ) {
        BooleanBuilder conditions = new BooleanBuilder();

        if (applicant != null) {
            conditions.and(memberAccount.name.contains(applicant));
        }
        if (agencyName != null) {
            conditions.and(agentApplication.requestAgencyName.contains(agencyName));
        }
        if (businessRegistrationNo != null) {
            conditions.and(agentApplication.requestBusinessNo.eq(businessRegistrationNo));
        }
        if (status != null) {
            conditions.and(agentApplication.status.eq(status));
        }
        return conditions;
    }
}
