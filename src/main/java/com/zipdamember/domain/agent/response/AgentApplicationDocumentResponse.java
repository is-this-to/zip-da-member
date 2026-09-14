package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.AgentApplicationDocumentType;
import com.zipdamember.domain.agent.constant.DocumentOcrStatus;
import com.zipdamember.domain.agent.entity.AgentApplicationDocument;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AgentApplicationDocumentResponse(
        String documentId,
        String fileId,
        AgentApplicationDocumentType documentType,
        DocumentOcrStatus ocrStatus,
        String ocrText,
        String businessRegistrationNo,
        LocalDate startDate,
        String representativeName,
        String agentRegistrationNo,
        String agencyName,
        String failureReason,
        String downloadUrl,
        LocalDateTime uploadedAt
) {
    public static AgentApplicationDocumentResponse of(
            AgentApplicationDocument document,
            String downloadUrl
    ) {
        return new AgentApplicationDocumentResponse(
                document.getDocumentId().toString(),
                document.getFileId().toString(),
                document.getDocumentType(),
                document.getOcrStatus(),
                document.getOcrText(),
                document.getOcrBusinessNo(),
                document.getOcrStartDate(),
                document.getOcrRepresentativeName(),
                document.getOcrAgentRegistrationNo(),
                document.getOcrAgencyName(),
                document.getOcrFailureReason(),
                downloadUrl,
                document.getUploadedAt()
        );
    }
}
