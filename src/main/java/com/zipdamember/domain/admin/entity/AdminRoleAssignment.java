package com.zipdamember.domain.admin.entity;

import com.github.f4b6a3.tsid.TsidCreator;
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
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "admin_role_assignment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE admin_role_assignment SET deleted_at = CURRENT_TIMESTAMP WHERE assignment_id = ?")
@Filter(name = "softDelete", condition = "deleted_at IS NULL")
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

    @PrePersist
    private void generateAssignmentId() {
        if (assignmentId == null) {
            assignmentId = TsidCreator.getTsid().toLong();
        }
    }
}
