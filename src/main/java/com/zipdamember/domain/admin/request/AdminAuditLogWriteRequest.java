package com.zipdamember.domain.admin.request;

import com.zipdamember.domain.admin.constant.AdminAuditAction;
import com.zipdamember.domain.admin.constant.AdminAuditActorType;
import com.zipdamember.domain.admin.constant.AdminAuditTargetService;
import com.zipdamember.domain.admin.constant.AdminAuditValueType;
import com.zipdamember.domain.admin.constant.AdminRoleCode;

import java.util.List;

public record AdminAuditLogWriteRequest(
        Long adminId,
        AdminAuditActorType actorType,
        AdminRoleCode roleCode,
        AdminAuditAction action,
        AdminAuditTargetService targetService,
        String targetType,
        String targetId,
        String reason,
        String ipAddress,
        String userAgent,
        List<Change> changes
) {
    public AdminAuditLogWriteRequest {
        changes = changes == null ? List.of() : List.copyOf(changes);
    }

    public record Change(
            String fieldName,
            String beforeValue,
            String afterValue,
            AdminAuditValueType valueType,
            Integer displayOrder
    ) {
    }
}
