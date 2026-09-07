package com.zipdamember.domain.admin.entity;

import com.zipdamember.domain.admin.constant.AdminAuditAction;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuditLogTest {

    @Test
    void action_usesStringEnumMapping() throws NoSuchFieldException {
        var actionField = AdminAuditLog.class.getDeclaredField("action");
        var enumerated = actionField.getAnnotation(Enumerated.class);

        assertThat(actionField.getType()).isEqualTo(AdminAuditAction.class);
        assertThat(enumerated).isNotNull();
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
    }

    @Test
    void change_usesSeparateTableAndAuditLogId() throws NoSuchFieldException {
        var table = AdminAuditLogChange.class.getAnnotation(Table.class);
        var auditLogId = AdminAuditLogChange.class.getDeclaredField("auditLogId");

        assertThat(table.name()).isEqualTo("admin_audit_log_change");
        assertThat(auditLogId.getType()).isEqualTo(Long.class);
        assertThat(AdminAuditLog.class.getDeclaredFields())
                .noneMatch(field -> field.getName().equals("beforeData")
                        || field.getName().equals("afterData"));
    }
}
