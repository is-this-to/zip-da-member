package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.AgentApplicationStatus;
import com.zipdamember.domain.agent.entity.AgentApplication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AgentApplicationResponse(
        String applicationId,
        AgentApplicationStatus status,
        boolean editable,
        LocalDateTime submittedAt,
        String rejectReason,
        LocalDateTime supplementDeadline,
        String businessRegistrationNo,
        LocalDate startDate,
        String representativeName,
        String agentRegistrationNo,
        String agencyName,
        List<AgentApplicationDocumentResponse> documents
) {
    public static AgentApplicationResponse of(AgentApplication application, List<AgentApplicationDocumentResponse> documents) {
        return new AgentApplicationResponse(
                application.getApplicationId().toString(),
                application.getStatus(),
                application.isEditable(),
                application.getSubmittedAt(),
                application.getRejectReason(),
                application.getSupplementDeadline(),
                application.getRequestBusinessNo(),
                application.getRequestStartDate(),
                application.getRequestRepresentativeName(),
                application.getRequestAgencyRegistrationNo(),
                application.getRequestAgencyName(),
                documents
        );
    }
}
