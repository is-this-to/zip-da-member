package com.zipdamember.domain.member.response;

public record MemberPermissionResponse(
        boolean allowed,
        String reason
) {
    public static MemberPermissionResponse granted() {
        return new MemberPermissionResponse(true, null);
    }

    public static MemberPermissionResponse denied(String reason) {
        return new MemberPermissionResponse(false, reason);
    }
}
