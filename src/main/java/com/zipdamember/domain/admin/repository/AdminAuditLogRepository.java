package com.zipdamember.domain.admin.repository;

import com.zipdamember.domain.admin.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    List<AdminAuditLog> findAllByTargetTypeAndTargetIdInOrderByOccurredAtDesc(
            String targetType,
            Collection<String> targetIds
    );
}
