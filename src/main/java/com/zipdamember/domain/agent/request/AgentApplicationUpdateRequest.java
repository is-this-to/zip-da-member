package com.zipdamember.domain.agent.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AgentApplicationUpdateRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9-]{10,12}$")
        String businessRegistrationNo,

        @NotNull
        @PastOrPresent
        LocalDate startDate,

        @NotBlank
        @Size(max = 50)
        String representativeName,

        @NotBlank
        @Size(max = 20)
        String agentRegistrationNo,

        @NotBlank
        @Size(max = 50)
        String agencyName
) {
}
