package com.zipdamember.domain.agent.request;

import com.zipdamember.domain.agent.constant.AgentProfileImageUpdatePolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AgentPublicProfileUpdateRequest(
        @Size(max = 2000) String intro,
        AgentProfileImageUpdatePolicy profileImageAction,
        String profileFileId,
        List<@Valid AgentSpecialtyRequest> specialties,
        List<@Valid AgentBusinessHourRequest> businessHours
) {
}
