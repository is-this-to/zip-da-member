package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.entity.AgentSpecialty;

public record AgentSpecialtyResponse(
        String regionCode,
        String regionName,
        int displayOrder
) {
    public static AgentSpecialtyResponse from(AgentSpecialty specialty) {
        return new AgentSpecialtyResponse(
                specialty.getRegionCode(),
                specialty.getRegionName(),
                specialty.getDisplayOrder()
        );
    }
}
