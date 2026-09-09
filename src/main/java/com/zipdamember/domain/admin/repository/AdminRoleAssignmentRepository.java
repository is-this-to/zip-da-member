package com.zipdamember.domain.admin.repository;

import com.zipdamember.domain.admin.entity.AdminRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AdminRoleAssignmentRepository extends JpaRepository<AdminRoleAssignment, Long> {
    List<AdminRoleAssignment> findAllByAdminIdAndDeletedAtIsNull(Long adminId);

    List<AdminRoleAssignment> findAllByAdminIdIn(Collection<Long> adminIds);
}
