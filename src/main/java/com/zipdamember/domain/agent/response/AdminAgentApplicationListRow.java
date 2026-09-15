package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.AgentApplicationStatus;
import com.zipdamember.domain.agent.constant.VerificationResultStatus;

import java.time.LocalDateTime;

public record AdminAgentApplicationListRow(
        Long applicationId,
        String applicantName,
        String agencyName,
        String businessRegistrationNo,
        String agencyRegistrationNo,
        AgentApplicationStatus status,
        VerificationResultStatus businessVerificationResult,
        VerificationResultStatus agencyRegistrationVerificationResult,
        LocalDateTime submittedAt
) {
}
