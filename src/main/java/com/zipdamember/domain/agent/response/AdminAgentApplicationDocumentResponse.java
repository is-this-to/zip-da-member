package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.AgentApplicationDocumentType;
import com.zipdamember.domain.agent.constant.DocumentOcrStatus;
import com.zipdamember.domain.agent.entity.AgentApplicationDocument;

import java.time.LocalDateTime;
import java.time.LocalDate;

public record AdminAgentApplicationDocumentResponse(
        String documentId,
        AgentApplicationDocumentType documentType,
        LocalDateTime uploadedAt,
        LocalDateTime verifiedAt,
        DocumentOcrStatus ocrStatus,
        String ocrText,
        String businessRegistrationNo,
        LocalDate startDate,
        String representativeName,
        String agentRegistrationNo,
        String agencyName,
        String ocrFailureReason,
        String downloadUrl
) {
    public static AdminAgentApplicationDocumentResponse of(
            AgentApplicationDocument document,
            String downloadUrl
    ) {
        return new AdminAgentApplicationDocumentResponse(
                document.getDocumentId().toString(),
                document.getDocumentType(),
                document.getUploadedAt(),
                document.getVerifiedAt(),
                document.getOcrStatus(),
                document.getOcrText(),
                document.getOcrBusinessNo(),
                document.getOcrStartDate(),
                document.getOcrRepresentativeName(),
                document.getOcrAgentRegistrationNo(),
                document.getOcrAgencyName(),
                document.getOcrFailureReason(),
                downloadUrl
        );
    }
}
