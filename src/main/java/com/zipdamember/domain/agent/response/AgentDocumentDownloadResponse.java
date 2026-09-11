package com.zipdamember.domain.agent.response;

public record AgentDocumentDownloadResponse(
        String url,
        int expiresInSeconds
) {
}
