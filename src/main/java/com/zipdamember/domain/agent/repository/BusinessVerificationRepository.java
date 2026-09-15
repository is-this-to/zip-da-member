package com.zipdamember.domain.agent.repository;

import com.zipdamember.domain.agent.entity.BusinessVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessVerificationRepository extends JpaRepository<BusinessVerification, Long> {

    Optional<BusinessVerification> findTopByApplicationIdOrderByVerificationIdDesc(Long applicationId);
}
