package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.AgentApplicationStatus;
import com.zipdamember.domain.agent.entity.AgentApplication;
import com.zipdamember.domain.agent.entity.AgentProfile;

import java.time.LocalDateTime;

public record AdminAgentApplicationApproveResponse(
        String applicationId,
        AgentApplicationStatus status,
        String agentId,
        LocalDateTime approvedAt,
        String reviewerAdminId
) {
    public static AdminAgentApplicationApproveResponse of(
            AgentApplication application,
            AgentProfile agentProfile
    ) {
        return new AdminAgentApplicationApproveResponse(
                application.getApplicationId().toString(),
                application.getStatus(),
                agentProfile.getAgentId().toString(),
                agentProfile.getApprovedAt(),
                application.getReviewerAdminId().toString()
        );
    }
}
