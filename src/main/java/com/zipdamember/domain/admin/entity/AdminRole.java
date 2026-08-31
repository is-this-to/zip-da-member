package com.zipdamember.domain.admin.entity;

import com.zipdamember.global.constant.AdminRoleCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "admin_role")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminRole {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, updatable = false, length = 30)
    private AdminRoleCode roleCode;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "description", length = 255)
    private String description;
}
