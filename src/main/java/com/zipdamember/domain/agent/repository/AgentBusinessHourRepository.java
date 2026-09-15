package com.zipdamember.domain.agent.repository;

import com.zipdamember.domain.agent.entity.AgentBusinessHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentBusinessHourRepository extends JpaRepository<AgentBusinessHour, Long> {
    List<AgentBusinessHour> findAllByAgentIdOrderByDayOfWeekAsc(Long agentId);
    void deleteAllByAgentId(Long agentId);
}
