package com.zipdamember.domain.admin.service;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.admin.constant.AdminAuditAction;
import com.zipdamember.domain.admin.constant.AdminAuditActorType;
import com.zipdamember.domain.admin.constant.AdminAuditTargetService;
import com.zipdamember.domain.admin.constant.AdminAuditValueType;
import com.zipdamember.domain.admin.entity.Admin;
import com.zipdamember.domain.admin.entity.AdminRoleAssignment;
import com.zipdamember.domain.admin.repository.AdminAccountQueryRepository;
import com.zipdamember.domain.admin.repository.AdminRoleAssignmentRepository;
import com.zipdamember.domain.admin.repository.AdminRepository;
import com.zipdamember.domain.admin.request.AdminAccountCreateRequest;
import com.zipdamember.domain.admin.request.AdminAccountSearchRequest;
import com.zipdamember.domain.admin.request.AdminAuditLogWriteRequest;
import com.zipdamember.domain.admin.response.AdminAccountCreateResponse;
import com.zipdamember.domain.admin.response.AdminAccountListResponse;
import com.zipdamember.domain.admin.response.AdminAccountResponse;
import com.zipdamember.global.error.custom.business.DuplicatedResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAccountService {
    private static final String INITIAL_PASSWORD_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz"
            + "0123456789!@#$";
    private static final int INITIAL_PASSWORD_LENGTH = 20;

    private final AdminAccountQueryRepository adminAccountQueryRepository;
    private final AdminRoleAssignmentRepository adminRoleAssignmentRepository;
    private final AdminRepository adminRepository;
    private final AdminAuditLogService adminAuditLogService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminAccountCreateResponse create(
            AdminAccountCreateRequest request,
            Long operatorAdminId,
            String ipAddress,
            String userAgent
    ) {
        // 관리자 코드 중복 검증
        if (adminRepository.existsByAdminCode(request.adminCode())) {
            throw new DuplicatedResourceException("이미 사용 중인 관리자 코드입니다.");
        }

        // 초기 비밀번호 난수 생성
        String initialPassword = generateInitialPassword();

        // 관리자 계정 생성
        Admin admin = Admin.create(
                request.adminCode(),
                passwordEncoder.encode(initialPassword),
                request.adminName()
        );
        Admin savedAdmin = adminRepository.save(admin);

        // 최초 관리자 역할 부여
        AdminRoleAssignment assignment = AdminRoleAssignment.create(
                savedAdmin.getAdminId(),
                request.adminRole(),
                operatorAdminId
        );
        adminRoleAssignmentRepository.save(assignment);

        // 관리자 계정 생성 감사 로그 저장
        adminAuditLogService.recordSuccess(new AdminAuditLogWriteRequest(
                operatorAdminId,
                AdminAuditActorType.ADMIN,
                AdminRoleCode.SUPER_ADMIN,
                AdminAuditAction.ADMIN_ACCOUNT_CREATE,
                AdminAuditTargetService.MEMBER,
                "ADMIN",
                savedAdmin.getAdminId().toString(),
                "관리자 계정 생성",
                ipAddress,
                userAgent,
                List.of(
                        new AdminAuditLogWriteRequest.Change(
                                "adminCode",
                                null,
                                savedAdmin.getAdminCode(),
                                AdminAuditValueType.STRING,
                                0
                        ),
                        new AdminAuditLogWriteRequest.Change(
                                "adminName",
                                null,
                                savedAdmin.getAdminName(),
                                AdminAuditValueType.STRING,
                                1
                        ),
                        new AdminAuditLogWriteRequest.Change(
                                "adminRole",
                                null,
                                request.adminRole().name(),
                                AdminAuditValueType.ENUM,
                                2
                        ),
                        new AdminAuditLogWriteRequest.Change(
                                "passwordChangeRequired",
                                null,
                                Boolean.TRUE.toString(),
                                AdminAuditValueType.BOOLEAN,
                                3
                        )
                )
        ));

        // 생성 관리자 응답 구성
        return AdminAccountCreateResponse.of(
                savedAdmin,
                assignment.getRoleCode(),
                initialPassword
        );
    }

    @Transactional(readOnly = true)
    public AdminAccountListResponse search(AdminAccountSearchRequest request) {
        // 페이지 조건 구성
        PageRequest pageable = PageRequest.of(
            request.page(),
            request.size(),
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("adminId"))
        );

        // 검색어 패턴 구성
        String pattern = toLikePattern(request.keyword());

        // 관리자 목록 조회
        Page<Admin> admins = adminAccountQueryRepository.searchAdmins(pattern, request.role(), pageable);

        // 관리자 식별자 추출
        List<Long> adminIds = admins.getContent().stream().map(Admin::getAdminId).toList();

        // 활성 권한 조회
        Map<Long, List<AdminRoleCode>> rolesByAdminId = findRolesByAdminId(adminIds);

        // 최근 로그인 조회
        Map<Long, LocalDateTime> lastLoginByAdminId = findLastLoginByAdminId(adminIds);

        // 응답 목록 구성
        List<AdminAccountResponse> content = admins.getContent().stream()
            .map(admin -> AdminAccountResponse.of(
                admin,
                rolesByAdminId.getOrDefault(admin.getAdminId(), List.of()),
                lastLoginByAdminId.get(admin.getAdminId())
            ))
            .toList();

        // 페이지 응답 구성
        return new AdminAccountListResponse(
            content,
            admins.getNumber(),
            admins.getSize(),
            admins.getTotalElements(),
            admins.getTotalPages()
        );
    }

    private Map<Long, List<AdminRoleCode>> findRolesByAdminId(List<Long> adminIds) {
        // 빈 관리자 목록
        if (adminIds.isEmpty()) {
            return Map.of();
        }

        // 관리자별 권한 구성
        Map<Long, List<AdminRoleCode>> result = new HashMap<>();
        adminRoleAssignmentRepository.findAllByAdminIdIn(adminIds)
            .forEach(assignment -> result
                .computeIfAbsent(assignment.getAdminId(), ignored -> new ArrayList<>())
                .add(assignment.getRoleCode()));

        // 권한 표시 순서 정렬
        result.values().forEach(roles -> roles.sort(Comparator.comparingInt(AdminRoleCode::ordinal)));
        return result;
    }

    private Map<Long, LocalDateTime> findLastLoginByAdminId(List<Long> adminIds) {
        // 빈 관리자 목록
        if (adminIds.isEmpty()) {
            return Map.of();
        }

        // 관리자별 최근 로그인 구성
        Map<Long, LocalDateTime> result = new HashMap<>();
        adminAccountQueryRepository.findLastSuccessfulLogins(adminIds)
            .forEach(login -> result.put(login.adminId(), login.lastLoginAt()));
        return result;
    }

    private String toLikePattern(String keyword) {
        // 빈 검색어 처리
        if (keyword == null) {
            return null;
        }

        // LIKE 특수문자 이스케이프
        return "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private String generateInitialPassword() {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder password = new StringBuilder(INITIAL_PASSWORD_LENGTH);

        // 초기 비밀번호 난수 구성
        for (int index = 0; index < INITIAL_PASSWORD_LENGTH; index++) {
            int randomIndex = secureRandom.nextInt(INITIAL_PASSWORD_CHARACTERS.length());
            password.append(INITIAL_PASSWORD_CHARACTERS.charAt(randomIndex));
        }

        return password.toString();
    }
}
