package com.zipdamember.domain.admin.repository;

import com.zipdamember.domain.admin.entity.AdminAuditLogChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AdminAuditLogChangeRepository extends JpaRepository<AdminAuditLogChange, Long> {

    List<AdminAuditLogChange> findAllByAuditLogIdInAndFieldName(
            Collection<Long> auditLogIds,
            String fieldName
    );
}
