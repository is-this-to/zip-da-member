package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.AgentApplicationDocumentType;
import com.zipdamember.domain.agent.entity.AgentApplicationDocument;

import java.time.LocalDateTime;

public record AdminAgentApplicationDocumentResponse(
        String documentId,
        AgentApplicationDocumentType documentType,
        LocalDateTime uploadedAt,
        LocalDateTime verifiedAt
) {
    public static AdminAgentApplicationDocumentResponse from(AgentApplicationDocument document) {
        return new AdminAgentApplicationDocumentResponse(
                document.getDocumentId().toString(),
                document.getDocumentType(),
                document.getUploadedAt(),
                document.getVerifiedAt()
        );
    }
}
