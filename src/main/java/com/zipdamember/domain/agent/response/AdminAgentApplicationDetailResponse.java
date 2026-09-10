package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.AgentApplicationStatus;
import com.zipdamember.domain.agent.entity.AgentApplication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminAgentApplicationDetailResponse(
        String applicationId,
        String applicantName,
        AgentApplicationStatus status,
        LocalDateTime submittedAt,
        LocalDateTime supplementDeadline,
        String businessRegistrationNo,
        String agencyRegistrationNo,
        String agencyName,
        LocalDate startDate,
        String representativeName,
        AdminBusinessVerificationResponse businessVerification,
        AdminAgencyRegistrationVerificationResponse agencyRegistrationVerification,
        List<AdminAgentApplicationDocumentResponse> documents
) {
    public static AdminAgentApplicationDetailResponse of(
            AgentApplication application,
            String applicantName,
            AdminBusinessVerificationResponse businessVerification,
            AdminAgencyRegistrationVerificationResponse agencyRegistrationVerification,
            List<AdminAgentApplicationDocumentResponse> documents
    ) {
        return new AdminAgentApplicationDetailResponse(
                application.getApplicationId().toString(),
                applicantName,
                application.getStatus(),
                application.getSubmittedAt(),
                application.getSupplementDeadline(),
                application.getRequestBusinessNo(),
                application.getRequestAgencyRegistrationNo(),
                application.getRequestAgencyName(),
                application.getRequestStartDate(),
                application.getRequestRepresentativeName(),
                businessVerification,
                agencyRegistrationVerification,
                documents
        );
    }
}
