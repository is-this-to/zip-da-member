package com.zipdamember.domain.agent.repository;

import com.zipdamember.domain.agent.entity.AgencyRegistrationVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgencyRegistrationVerificationRepository
        extends JpaRepository<AgencyRegistrationVerification, Long> {
}
