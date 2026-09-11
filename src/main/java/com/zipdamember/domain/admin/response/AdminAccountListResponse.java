package com.zipdamember.domain.admin.response;

import java.util.List;

public record AdminAccountListResponse(
    List<AdminAccountResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public AdminAccountListResponse {
        content = List.copyOf(content);
    }
}
