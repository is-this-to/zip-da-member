package com.zipdamember.domain.admin.repository;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.admin.entity.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRoleRepository extends JpaRepository<AdminRole, AdminRoleCode> {
}
