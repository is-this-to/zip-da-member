package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.AgentApplicationStatus;
import com.zipdamember.domain.agent.entity.AgentApplication;

import java.time.LocalDateTime;

public record AdminAgentApplicationSupplementResponse(
        String applicationId,
        AgentApplicationStatus status,
        String supplementReason,
        LocalDateTime supplementDeadline,
        String reviewerAdminId
) {
    public static AdminAgentApplicationSupplementResponse from(AgentApplication application) {
        return new AdminAgentApplicationSupplementResponse(
                application.getApplicationId().toString(),
                application.getStatus(),
                application.getRejectReason(),
                application.getSupplementDeadline(),
                application.getReviewerAdminId().toString()
        );
    }
}
