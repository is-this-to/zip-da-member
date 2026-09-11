package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.AgentOperatingStatus;
import com.zipdamember.domain.agent.entity.AgentProfile;

import java.time.LocalDateTime;

public record AdminAgentOperatingStatusResponse(
        String agentId,
        String agencyName,
        AgentOperatingStatus operatingStatus,
        String statusChangedBy,
        LocalDateTime statusChangedAt
) {
    public static AdminAgentOperatingStatusResponse from(AgentProfile profile) {
        return new AdminAgentOperatingStatusResponse(
                profile.getAgentId().toString(),
                profile.getAgencyName(),
                profile.getOperatingStatus(),
                profile.getStatusChangedBy() == null ? null : profile.getStatusChangedBy().toString(),
                profile.getStatusChangedAt()
        );
    }
}
