package com.zipdamember.domain.admin.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.f4b6a3.tsid.TsidCreator;
import com.zipdamember.global.constant.AdminAuditActorType;
import com.zipdamember.global.constant.AdminAuditResult;
import com.zipdamember.global.constant.AdminAuditTargetService;
import com.zipdamember.global.constant.AdminRoleCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_service", length = 30)
    private AdminAuditTargetService targetService = AdminAuditTargetService.MEMBER;

    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", columnDefinition = "JSON")
    private JsonNode beforeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", columnDefinition = "JSON")
    private JsonNode afterData;

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

    @PrePersist
    private void generateAuditLogId() {
        if (auditLogId == null) {
            auditLogId = TsidCreator.getTsid().toLong();
        }
    }
}
