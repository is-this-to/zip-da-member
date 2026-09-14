package com.zipdamember.domain.member.response;

import com.zipdamember.domain.agent.constant.AgentOperatingStatus;
import com.zipdamember.domain.agent.entity.AgentProfile;

public record MemberAgentSummaryResponse(
        String agentId,
        String agencyName,
        String representativeName,
        AgentOperatingStatus operatingStatus
) {
    public static MemberAgentSummaryResponse from(AgentProfile profile) {
        return new MemberAgentSummaryResponse(
                profile.getAgentId().toString(),
                profile.getAgencyName(),
                profile.getRepresentativeName(),
                profile.getOperatingStatus()
        );
    }
}
