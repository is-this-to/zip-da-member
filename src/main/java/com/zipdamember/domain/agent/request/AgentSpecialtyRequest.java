package com.zipdamember.domain.agent.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentSpecialtyRequest(
        @NotBlank @Size(max = 20) String regionCode,
        @NotBlank @Size(max = 100) String regionName,
        @Min(0) int displayOrder
) {
}
