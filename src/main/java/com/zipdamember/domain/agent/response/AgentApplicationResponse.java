package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.AgentApplicationStatus;
import com.zipdamember.domain.agent.entity.AgentApplication;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "공인중개사 신청 응답")
public record AgentApplicationResponse(
        @Schema(description = "공인중개사 신청 회원 식별자")
        String applicationId,

        @Schema(description = "공인중개사 신청 상태")
        AgentApplicationStatus status,

        @Schema(description = "편집 가능 상태")
        boolean editable,

        @Schema(description = "제출했을때 신청 일자")
        LocalDateTime submittedAt,

        @Schema(description = "공인중개사 신청 응답")
        String rejectReason,

        @Schema(description = "공인중개사 신청 응답")
        LocalDateTime supplementDeadline,

        @Schema(description = "공인중개사 신청 응답")
        String businessRegistrationNo,

        @Schema(description = "공인중개사 신청 응답")
        LocalDate startDate,

        @Schema(description = "공인중개사 신청 응답")
        String representativeName,

        @Schema(description = "공인중개사 신청 응답")
        String agentRegistrationNo,

        @Schema(description = "공인중개사 신청 응답")
        String agencyName,

        @Schema(description = "공인중개사 신청 응답")
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
