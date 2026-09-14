package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.AgentOperatingStatus;
import com.zipdamember.domain.agent.entity.AgentProfile;

import java.util.List;

public record AgentPublicProfileResponse(
        String agentId,
        String agencyName,
        String representativeName,
        String intro,
        String profileImageUrl,
        String phone,
        String address,
        AgentOperatingStatus operatingStatus,
        List<AgentSpecialtyResponse> specialties,
        List<AgentBusinessHourResponse> businessHours
) {
    public static AgentPublicProfileResponse from(
            AgentProfile profile,
            String profileImageUrl,
            List<AgentSpecialtyResponse> specialties,
            List<AgentBusinessHourResponse> businessHours
    ) {
        return new AgentPublicProfileResponse(
                profile.getAgentId().toString(),
                profile.getAgencyName(),
                profile.getRepresentativeName(),
                profile.getIntro(),
                profileImageUrl,
                profile.getPhone(),
                profile.getAddress(),
                profile.getOperatingStatus(),
                specialties,
                businessHours
        );
    }
}
