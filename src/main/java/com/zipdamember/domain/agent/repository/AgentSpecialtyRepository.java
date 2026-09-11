package com.zipdamember.domain.agent.repository;

import com.zipdamember.domain.agent.entity.AgentSpecialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentSpecialtyRepository extends JpaRepository<AgentSpecialty, Long> {
    List<AgentSpecialty> findAllByAgentIdOrderByDisplayOrderAsc(Long agentId);
    void deleteAllByAgentId(Long agentId);
}
