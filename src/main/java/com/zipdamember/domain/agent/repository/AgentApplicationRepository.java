package com.zipdamember.domain.agent.repository;

import com.zipdamember.domain.agent.entity.AgentApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentApplicationRepository extends JpaRepository<AgentApplication, Long> {
}
