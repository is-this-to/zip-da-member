package com.zipdamember.domain.agent.request;

import com.zipdamember.domain.agent.constant.AgentOperatingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAgentOperatingStatusChangeRequest(
        @NotNull AgentOperatingStatus operatingStatus,
        @NotBlank @Size(max = 500) String reason
) {
    public AdminAgentOperatingStatusChangeRequest {
        reason = reason == null ? null : reason.strip();
    }
}
