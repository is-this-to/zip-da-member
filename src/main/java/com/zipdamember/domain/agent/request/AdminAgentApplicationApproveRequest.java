package com.zipdamember.domain.agent.request;

import jakarta.validation.constraints.Size;

public record AdminAgentApplicationApproveRequest(
        @Size(max = 500)
        String reviewNote
) {
    public AdminAgentApplicationApproveRequest {
        reviewNote = reviewNote == null ? null : reviewNote.strip();
    }
}
