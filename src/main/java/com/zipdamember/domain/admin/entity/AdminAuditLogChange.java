package com.zipdamember.domain.admin.entity;

import com.zipdamember.domain.admin.constant.AdminAuditValueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "admin_audit_log_change",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_admin_audit_change_field",
                columnNames = {"audit_log_id", "field_name"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLogChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_change_id", nullable = false, updatable = false,
            columnDefinition = "BIGINT UNSIGNED")
    private Long auditChangeId;

    @Column(name = "audit_log_id", nullable = false, updatable = false,
            columnDefinition = "BIGINT UNSIGNED")
    private Long auditLogId;

    @Column(name = "field_name", nullable = false, updatable = false, length = 100)
    private String fieldName;

    @Column(name = "before_value", updatable = false, columnDefinition = "TEXT")
    private String beforeValue;

    @Column(name = "after_value", updatable = false, columnDefinition = "TEXT")
    private String afterValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, updatable = false, length = 20)
    private AdminAuditValueType valueType = AdminAuditValueType.STRING;

    @Column(name = "display_order", nullable = false, updatable = false,
            columnDefinition = "SMALLINT UNSIGNED")
    private Integer displayOrder = 0;

    public static AdminAuditLogChange create(
            Long auditLogId,
            String fieldName,
            String beforeValue,
            String afterValue,
            AdminAuditValueType valueType,
            Integer displayOrder
    ) {
        AdminAuditLogChange auditLogChange = new AdminAuditLogChange();
        auditLogChange.auditLogId = auditLogId;
        auditLogChange.fieldName = fieldName;
        auditLogChange.beforeValue = beforeValue;
        auditLogChange.afterValue = afterValue;
        auditLogChange.valueType = valueType;
        auditLogChange.displayOrder = displayOrder;
        return auditLogChange;
    }
}
