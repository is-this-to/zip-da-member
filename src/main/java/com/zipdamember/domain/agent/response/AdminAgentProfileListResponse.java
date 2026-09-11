package com.zipdamember.domain.agent.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminAgentProfileListResponse(
        List<AdminAgentProfileResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AdminAgentProfileListResponse from(Page<AdminAgentProfileResponse> result) {
        return new AdminAgentProfileListResponse(
                List.copyOf(result.getContent()), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()
        );
    }
}
