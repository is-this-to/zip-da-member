package com.zipdamember.domain.member.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminMemberListResponse(
        List<AdminMemberResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AdminMemberListResponse from(Page<AdminMemberResponse> result) {
        return new AdminMemberListResponse(
                List.copyOf(result.getContent()), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()
        );
    }
}
