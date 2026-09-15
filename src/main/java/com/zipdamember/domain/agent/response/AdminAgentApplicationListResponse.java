package com.zipdamember.domain.agent.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminAgentApplicationListResponse(
        List<AdminAgentApplicationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AdminAgentApplicationListResponse from(Page<AdminAgentApplicationListRow> applications) {
        return new AdminAgentApplicationListResponse(
                applications.getContent().stream()
                        .map(AdminAgentApplicationResponse::from)
                        .toList(),
                applications.getNumber(),
                applications.getSize(),
                applications.getTotalElements(),
                applications.getTotalPages()
        );
    }
}
