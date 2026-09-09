package com.zipdamember.domain.agent.repository;

import com.zipdamember.domain.agent.entity.AgentApplication;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface AgentApplicationRepository extends JpaRepository<AgentApplication, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AgentApplication> findByApplicationId(Long applicationId);
}
