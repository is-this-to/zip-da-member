package com.zipdamember.domain.agent.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AdminAgentApplicationSupplementRequest(
        @NotBlank
        @Size(max = 500)
        String supplementReason,

        @NotNull
        @Future
        LocalDateTime supplementDeadline
) {
    public AdminAgentApplicationSupplementRequest {
        supplementReason = supplementReason == null ? null : supplementReason.strip();
    }
}
