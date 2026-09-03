package com.zipdamember.domain.admin.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.zipdamember.domain.admin.constant.AdminRoleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "admin_role_assignment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE admin_role_assignment SET deleted_at = CURRENT_TIMESTAMP WHERE assignment_id = ? AND deleted_at IS NULL")
@FilterDef(name = "softDelete")
@Filter(name = "softDelete", condition = "deleted_at IS NULL")
@Getter
public class AdminRoleAssignment {
    @Id
    @Column(
            name = "assignment_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BIGINT UNSIGNED"
    )
    private Long assignmentId;

    @Column(
            name = "admin_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BIGINT UNSIGNED"
    )
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, length = 30)
    private AdminRoleCode roleCode;

    @CreatedDate
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by", updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long assignedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static AdminRoleAssignment create(
            Long adminId,
            AdminRoleCode roleCode,
            Long assignedBy
    ) {
        AdminRoleAssignment assignment = new AdminRoleAssignment();
        assignment.adminId = adminId;
        assignment.roleCode = roleCode;
        assignment.assignedBy = assignedBy;
        return assignment;
    }

    @PrePersist
    private void generateAssignmentId() {
        if (assignmentId == null) {
            assignmentId = TsidCreator.getTsid().toLong();
        }
    }
}
