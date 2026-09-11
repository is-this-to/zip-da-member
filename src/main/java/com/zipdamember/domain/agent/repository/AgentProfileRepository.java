package com.zipdamember.domain.agent.repository;

import com.zipdamember.domain.agent.entity.AgentProfile;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AgentProfileRepository extends Repository<AgentProfile, Long>,
        QuerydslPredicateExecutor<AgentProfile> {

    AgentProfile save(AgentProfile agentProfile);

    Optional<AgentProfile> findByAgentId(Long agentId);

    Optional<AgentProfile> findByMemberId(Long memberId);

    Page<AgentProfile> findAll(Pageable pageable);

    Page<AgentProfile> findAllByAgencyNameContaining(String agencyName, Pageable pageable);

    boolean existsByBusinessRegistrationNo(String businessRegistrationNo);

}
