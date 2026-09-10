package com.zipdamember.domain.agent.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminAgentOperatingStatusSearchRequest(
        @Size(max = 150) String agencyName,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size
) {
    public AdminAgentOperatingStatusSearchRequest {
        agencyName = agencyName == null || agencyName.isBlank() ? null : agencyName.strip();
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
