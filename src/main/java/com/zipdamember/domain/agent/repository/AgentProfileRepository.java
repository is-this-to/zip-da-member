package com.zipdamember.domain.agent.repository;

import com.zipdamember.domain.agent.entity.AgentProfile;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.Repository;

public interface AgentProfileRepository extends Repository<AgentProfile, Long>,
        QuerydslPredicateExecutor<AgentProfile> {

    AgentProfile save(AgentProfile agentProfile);

    boolean existsByBusinessRegistrationNo(String businessRegistrationNo);

}
