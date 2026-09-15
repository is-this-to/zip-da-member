package com.zipdamember.domain.agent.repository;

import com.zipdamember.domain.agent.entity.AgentApplication;
import com.zipdamember.domain.agent.constant.AgentApplicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface AgentApplicationRepository extends JpaRepository<AgentApplication, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AgentApplication> findByApplicationId(Long applicationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AgentApplication> findByApplicationIdAndMemberId(Long applicationId, Long memberId);

    Optional<AgentApplication> findTopByMemberIdOrderByCreatedAtDescApplicationIdDesc(Long memberId);

    List<AgentApplication> findTop100ByStatusAndSubmittedAtIsNotNullOrderBySubmittedAtAscApplicationIdAsc(
            AgentApplicationStatus status
    );
    boolean existsByRequestBusinessNoAndApplicationIdNotAndSubmittedAtIsNotNullAndStatusIn(
            String requestBusinessNo,
            Long applicationId,
            List<AgentApplicationStatus> statuses
    );

    boolean existsByRequestAgencyRegistrationNoAndApplicationIdNotAndSubmittedAtIsNotNullAndStatusIn(
            String requestAgencyRegistrationNo,
            Long applicationId,
            List<AgentApplicationStatus> statuses
    );
}
