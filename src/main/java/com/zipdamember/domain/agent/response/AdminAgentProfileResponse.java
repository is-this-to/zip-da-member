package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.AgentOperatingStatus;
import com.zipdamember.domain.agent.entity.AgentProfile;

import java.time.LocalDateTime;

public record AdminAgentProfileResponse(
        String agentId,
        String agencyName,
        String representativeName,
        String businessRegistrationNo,
        String address,
        AgentOperatingStatus operatingStatus,
        LocalDateTime approvedAt
) {
    public static AdminAgentProfileResponse from(AgentProfile profile) {
        return new AdminAgentProfileResponse(
                profile.getAgentId().toString(), profile.getAgencyName(),
                profile.getRepresentativeName(), profile.getBusinessRegistrationNo(),
                profile.getAddress(), profile.getOperatingStatus(), profile.getApprovedAt()
        );
    }
}
