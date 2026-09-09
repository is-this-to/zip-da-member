package com.zipdamember.domain.agent.request;

import com.zipdamember.domain.agent.constant.AgentApplicationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminAgentApplicationSearchRequest(
        @Size(max = 50)
        String applicant,

        @Size(max = 150)
        String agencyName,

        @Size(max = 50)
        String businessRegistrationNo,

        AgentApplicationStatus status,

        @Min(0)
        Integer page,

        @Min(1)
        @Max(100)
        Integer size
) {
    public AdminAgentApplicationSearchRequest {
        applicant = applicant == null || applicant.isBlank() ? null : applicant.strip();
        agencyName = agencyName == null || agencyName.isBlank() ? null : agencyName.strip();
        businessRegistrationNo = businessRegistrationNo == null || businessRegistrationNo.isBlank()
                ? null
                : businessRegistrationNo.strip();
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
