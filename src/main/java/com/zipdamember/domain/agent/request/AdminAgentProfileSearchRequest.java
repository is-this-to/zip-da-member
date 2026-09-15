package com.zipdamember.domain.agent.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminAgentProfileSearchRequest(
        @Size(max = 150) String agencyName,
        @Size(max = 50) String representativeName,
        @Size(max = 50) String businessRegistrationNo,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size
) {
    public AdminAgentProfileSearchRequest {
        agencyName = normalize(agencyName);
        representativeName = normalize(representativeName);
        businessRegistrationNo = normalize(businessRegistrationNo);
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
