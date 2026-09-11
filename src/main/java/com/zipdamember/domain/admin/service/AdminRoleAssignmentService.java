package com.zipdamember.domain.admin.service;

import com.zipdamember.domain.admin.constant.AdminAuditAction;
import com.zipdamember.domain.admin.constant.AdminAuditActorType;
import com.zipdamember.domain.admin.constant.AdminAuditTargetService;
import com.zipdamember.domain.admin.constant.AdminAuditValueType;
import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.admin.entity.AdminRoleAssignment;
import com.zipdamember.domain.admin.repository.AdminRepository;
import com.zipdamember.domain.admin.repository.AdminRoleAssignmentRepository;
import com.zipdamember.domain.admin.repository.AdminRoleRepository;
import com.zipdamember.domain.admin.request.AdminAuditLogWriteRequest;
import com.zipdamember.domain.auth.entity.AdminLoginSession;
import com.zipdamember.domain.auth.repository.AdminLoginSessionRepository;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.error.custom.business.DuplicatedResourceException;
import com.zipdamember.global.error.custom.business.NotFoundResourceException;
import com.zipdamember.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRoleAssignmentService {
    private final AdminRepository adminRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminRoleAssignmentRepository adminRoleAssignmentRepository;
    private final AdminLoginSessionRepository adminLoginSessionRepository;
    private final AdminAuditLogService adminAuditLogService;

    @Transactional
    public void assign(
            Long targetAdminId,
            AdminRoleCode roleCode,
            Long operatorAdminId,
            String ipAddress,
            String userAgent
    ) {
        // 대상 관리자 존재 검증
        validateAdminExists(targetAdminId);

        // 역할 코드 존재 검증
        validateRoleExists(roleCode);

        // 활성 역할 중복 검증
        if (adminRoleAssignmentRepository.existsByAdminIdAndRoleCode(targetAdminId, roleCode)) {
            throw new DuplicatedResourceException("이미 부여된 관리자 역할입니다.");
        }

        // 관리자 역할 할당 생성
        adminRoleAssignmentRepository.save(AdminRoleAssignment.create(
                targetAdminId,
                roleCode,
                operatorAdminId
        ));

        // 대상 관리자 활성 세션 폐기
        revokeActiveSessions(targetAdminId);

        // 관리자 역할 부여 감사 로그 저장
        saveAuditLog(
                operatorAdminId,
                AdminAuditAction.ADMIN_ROLE_ASSIGN,
                targetAdminId,
                "관리자 역할 부여",
                null,
                roleCode,
                ipAddress,
                userAgent
        );
    }

    @Transactional
    public void revoke(
            Long targetAdminId,
            AdminRoleCode roleCode,
            Long operatorAdminId,
            String ipAddress,
            String userAgent
    ) {
        // 대상 관리자 존재 검증
        validateAdminExists(targetAdminId);

        // 활성 역할 잠금 조회
        AdminRoleAssignment assignment = adminRoleAssignmentRepository
                .findByAdminIdAndRoleCode(targetAdminId, roleCode)
                .orElseThrow(() -> new NotFoundResourceException("회수할 관리자 역할을 찾을 수 없습니다."));

        // 최종 최고 관리자 역할 회수 검증
        validateLastSuperAdminRole(targetAdminId, roleCode, operatorAdminId);

        // 관리자 역할 소프트 삭제
        adminRoleAssignmentRepository.delete(assignment);

        // 대상 관리자 활성 세션 폐기
        revokeActiveSessions(targetAdminId);

        // 관리자 역할 회수 감사 로그 저장
        saveAuditLog(
                operatorAdminId,
                AdminAuditAction.ADMIN_ROLE_REVOKE,
                targetAdminId,
                "관리자 역할 회수",
                roleCode,
                null,
                ipAddress,
                userAgent
        );
    }

    private void validateAdminExists(Long adminId) {
        // 대상 관리자 존재 검증
        if (!adminRepository.existsById(adminId)) {
            throw new NotFoundResourceException("대상 관리자를 찾을 수 없습니다.");
        }
    }

    private void validateRoleExists(AdminRoleCode roleCode) {
        // 역할 코드 존재 검증
        if (!adminRoleRepository.existsById(roleCode)) {
            throw new NotFoundResourceException("등록되지 않은 관리자 역할입니다.");
        }
    }

    private void validateLastSuperAdminRole(
            Long targetAdminId,
            AdminRoleCode roleCode,
            Long operatorAdminId
    ) {
        // 최종 최고 관리자 역할 대상 검증
        if (roleCode != AdminRoleCode.SUPER_ADMIN || !targetAdminId.equals(operatorAdminId)) {
            return;
        }

        // 최고 관리자 역할 잠금 조회
        List<AdminRoleAssignment> superAdminAssignments = adminRoleAssignmentRepository
                .findAllByRoleCode(AdminRoleCode.SUPER_ADMIN);
        if (superAdminAssignments.size() <= 1) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "자신의 마지막 SUPER_ADMIN 역할은 회수할 수 없습니다."
            );
        }
    }

    private void revokeActiveSessions(Long adminId) {
        // 대상 관리자 활성 세션 조회
        List<AdminLoginSession> sessions = adminLoginSessionRepository
                .findAllByAdminIdAndRevokedAtIsNull(adminId);

        // 대상 관리자 활성 세션 폐기
        sessions.forEach(AdminLoginSession::revoke);
    }

    private void saveAuditLog(
            Long operatorAdminId,
            AdminAuditAction action,
            Long targetAdminId,
            String reason,
            AdminRoleCode beforeRole,
            AdminRoleCode afterRole,
            String ipAddress,
            String userAgent
    ) {
        // 역할 변경 감사 로그 저장
        adminAuditLogService.recordSuccess(new AdminAuditLogWriteRequest(
                operatorAdminId,
                AdminAuditActorType.ADMIN,
                AdminRoleCode.SUPER_ADMIN,
                action,
                AdminAuditTargetService.MEMBER,
                "ADMIN",
                targetAdminId.toString(),
                reason,
                ipAddress,
                userAgent,
                List.of(new AdminAuditLogWriteRequest.Change(
                        "roleCode",
                        beforeRole == null ? null : beforeRole.name(),
                        afterRole == null ? null : afterRole.name(),
                        AdminAuditValueType.ENUM,
                        0
                ))
        ));
    }
}
