package com.zipdamember.domain.admin.repository;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.admin.entity.AdminRoleAssignment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdminRoleAssignmentRepository extends JpaRepository<AdminRoleAssignment, Long> {
    List<AdminRoleAssignment> findAllByAdminId(Long adminId);

    List<AdminRoleAssignment> findAllByAdminIdIn(Collection<Long> adminIds);

    boolean existsByAdminIdAndRoleCode(Long adminId, AdminRoleCode roleCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AdminRoleAssignment> findByAdminIdAndRoleCode(Long adminId, AdminRoleCode roleCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AdminRoleAssignment> findAllByRoleCode(AdminRoleCode roleCode);
}
