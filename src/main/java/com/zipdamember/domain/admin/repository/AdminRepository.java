package com.zipdamember.domain.admin.repository;

import com.zipdamember.domain.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByAdminCode(String AdminCode);
}
