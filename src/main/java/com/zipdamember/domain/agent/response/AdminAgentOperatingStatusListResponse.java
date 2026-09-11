package com.zipdamember.domain.agent.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminAgentOperatingStatusListResponse(
        List<AdminAgentOperatingStatusResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AdminAgentOperatingStatusListResponse from(Page<AdminAgentOperatingStatusResponse> result) {
        return new AdminAgentOperatingStatusListResponse(
                List.copyOf(result.getContent()),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}
