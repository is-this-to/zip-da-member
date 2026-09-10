package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.AgentApplicationStatus;
import com.zipdamember.domain.agent.constant.VerificationResultStatus;

import java.time.LocalDateTime;

public record AdminAgentApplicationResponse(
        String applicationId,
        String applicantName,
        String agencyName,
        String businessRegistrationNo,
        String agencyRegistrationNo,
        AgentApplicationStatus status,
        VerificationResultStatus businessVerificationResult,
        VerificationResultStatus agencyRegistrationVerificationResult,
        LocalDateTime submittedAt
) {
    public static AdminAgentApplicationResponse from(AdminAgentApplicationListRow row) {
        return new AdminAgentApplicationResponse(
                row.applicationId().toString(),
                row.applicantName(),
                row.agencyName(),
                row.businessRegistrationNo(),
                row.agencyRegistrationNo(),
                row.status(),
                row.businessVerificationResult(),
                row.agencyRegistrationVerificationResult(),
                row.submittedAt()
        );
    }
}
