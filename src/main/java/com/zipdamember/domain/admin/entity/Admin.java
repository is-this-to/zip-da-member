package com.zipdamember.domain.admin.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "admin")
@SQLDelete(sql = "UPDATE admin SET deleted_at = NOW() WHERE admin_id = ?")
@FilterDef(name = "softDelete")
@Filter(name = "softDelete", condition = "deleted_at IS NULL")
@Getter
@Setter
public class Admin {
    @Id
    @Column(
        name = "admin_id",
        nullable = false,
        updatable = false,
        columnDefinition = "BIGINT UNSIGNED"
    )
    private Long adminId;

    @Column(
        name = "admin_code",
        nullable = false,
        unique = true,
        length = 20
    )
    private String adminCode;

    @Column(
        name = "admin_password",
        nullable = false,
        length = 255
    )
    private String adminPassword;

    @Column(
        name = "password_change_required",
        length = 255
    )
    private Boolean password_change_required;

    @Column(
        name = "admin_name",
        nullable = false,
        length = 50
    )
    private String adminName;

    @CreatedDate
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(
        name = "updated_at",
        nullable = false
    )
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    private void generateAdminId() {
        if (adminId == null) {
            adminId = TsidCreator.getTsid().toLong();
        }
    }
}
