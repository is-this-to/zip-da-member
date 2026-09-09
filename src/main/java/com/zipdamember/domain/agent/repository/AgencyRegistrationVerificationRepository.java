package com.zipdamember.domain.agent.repository;

import com.zipdamember.domain.agent.entity.AgencyRegistrationVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgencyRegistrationVerificationRepository
        extends JpaRepository<AgencyRegistrationVerification, Long> {

    Optional<AgencyRegistrationVerification> findTopByApplicationIdOrderByVerificationIdDesc(
            Long applicationId
    );
}
