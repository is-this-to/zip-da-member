package com.zipdamember.domain.admin.entity;

import com.zipdamember.domain.admin.constant.AdminAuditActorType;
import com.zipdamember.domain.admin.constant.AdminAuditAction;
import com.zipdamember.domain.admin.constant.AdminAuditResult;
import com.zipdamember.domain.admin.constant.AdminAuditTargetService;
import com.zipdamember.domain.admin.constant.AdminRoleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "admin_audit_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
            name = "audit_log_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BIGINT UNSIGNED"
    )
    private Long auditLogId;

    @Column(name = "admin_id", updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", length = 20)
    private AdminAuditActorType actorType = AdminAuditActorType.ADMIN;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", length = 30)
    private AdminRoleCode roleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 80)
    private AdminAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_service", length = 30)
    private AdminAuditTargetService targetService = AdminAuditTargetService.MEMBER;

    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private AdminAuditResult result;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreatedDate
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    public static AdminAuditLog create(
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
            String requestId,
            AdminAuditResult result
    ) {
        AdminAuditLog auditLog = new AdminAuditLog();
        auditLog.adminId = adminId;
        auditLog.actorType = actorType;
        auditLog.roleCode = roleCode;
        auditLog.action = action;
        auditLog.targetService = targetService;
        auditLog.targetType = targetType;
        auditLog.targetId = targetId;
        auditLog.reason = reason;
        auditLog.ipAddress = ipAddress;
        auditLog.userAgent = userAgent;
        auditLog.requestId = requestId;
        auditLog.result = result;
        return auditLog;
    }

}
