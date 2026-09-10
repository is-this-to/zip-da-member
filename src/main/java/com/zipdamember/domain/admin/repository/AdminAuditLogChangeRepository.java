package com.zipdamember.domain.admin.repository;

import com.zipdamember.domain.admin.entity.AdminAuditLogChange;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogChangeRepository extends JpaRepository<AdminAuditLogChange, Long> {
}
