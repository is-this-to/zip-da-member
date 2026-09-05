package com.zipdamember.domain.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminRoleAssignmentService {
/*
    private final AdminRepository adminRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminRoleAssignmentRepository adminRoleAssignmentRepository;
    private final AdminLoginSessionRepository adminLoginSessionRepository;

    @Transactional
    public Long assignRole(
            Long targetAdminId,
            AdminRoleCode roleCode,
            Long operatorAdminId
    ) {
        validateSuperAdmin(operatorAdminId);
        adminRepository.findActiveByIdForUpdate(targetAdminId)
                .orElseThrow(() -> new NotFoundResourceException("역할을 부여할 관리자를 찾을 수 없습니다."));
        validateRoleExists(roleCode);

        if (adminRoleAssignmentRepository.existsByAdminIdAndRoleCodeAndDeletedAtIsNull(
                targetAdminId,
                roleCode
        )) {
            throw new DuplicatedResourceException("이미 부여된 관리자 역할입니다.");
        }

        AdminRoleAssignment assignment = AdminRoleAssignment.create(
                targetAdminId,
                roleCode,
                operatorAdminId
        );
        adminRoleAssignmentRepository.save(assignment);
        adminLoginSessionRepository.deleteAllByAdminId(targetAdminId);

        // TODO: [관리자 감사 로그] 관리자 역할 부여 및 기존 세션 폐기 기록
        return assignment.getAssignmentId();
    }

    @Transactional
    public void revokeRole(
            Long targetAdminId,
            AdminRoleCode roleCode,
            Long operatorAdminId
    ) {
        validateSuperAdmin(operatorAdminId);
        adminRepository.findActiveByIdForUpdate(targetAdminId)
                .orElseThrow(() -> new NotFoundResourceException("역할을 회수할 관리자를 찾을 수 없습니다."));

        AdminRoleAssignment assignment = adminRoleAssignmentRepository.findActiveRoleForUpdate(
                        targetAdminId,
                        roleCode
                )
                .orElseThrow(() -> new NotFoundResourceException("회수할 관리자 역할을 찾을 수 없습니다."));

        validateLastSuperAdminRole(targetAdminId, roleCode, operatorAdminId);

        assignment.revoke();
        adminLoginSessionRepository.deleteAllByAdminId(targetAdminId);

        // TODO: [관리자 감사 로그] 관리자 역할 회수 및 기존 세션 폐기 기록
    }

    private void validateSuperAdmin(Long operatorAdminId) {
        adminRepository.findByAdminIdAndDeletedAtIsNull(operatorAdminId)
                .orElseThrow(() -> new NotFoundResourceException("작업 관리자를 찾을 수 없습니다."));

        if (!adminRoleAssignmentRepository.existsByAdminIdAndRoleCodeAndDeletedAtIsNull(
                operatorAdminId,
                AdminRoleCode.SUPER_ADMIN
        )) {
            throw new BusinessException(
                    CustomResponseCode.UNAUTHORIZED_ERROR,
                    "관리자 역할을 변경할 권한이 없습니다."
            );
        }
    }

    private void validateRoleExists(AdminRoleCode roleCode) {
        if (!adminRoleRepository.existsById(roleCode)) {
            throw new NotFoundResourceException("등록되지 않은 관리자 역할입니다.");
        }
    }

    private void validateLastSuperAdminRole(
            Long targetAdminId,
            AdminRoleCode roleCode,
            Long operatorAdminId
    ) {
        if (roleCode != AdminRoleCode.SUPER_ADMIN || !targetAdminId.equals(operatorAdminId)) {
            return;
        }

        if (adminRoleAssignmentRepository.countByRoleCodeAndDeletedAtIsNull(
                AdminRoleCode.SUPER_ADMIN
        ) <= 1) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "자신의 마지막 SUPER_ADMIN 역할은 회수할 수 없습니다."
            );
        }
    }*/
}
