package com.zipdamember.domain.admin.service;

import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.admin.entity.Admin;
import com.zipdamember.domain.admin.repository.AdminAccountQueryRepository;
import com.zipdamember.domain.admin.repository.AdminRoleAssignmentRepository;
import com.zipdamember.domain.admin.request.AdminAccountSearchRequest;
import com.zipdamember.domain.admin.response.AdminAccountListResponse;
import com.zipdamember.domain.admin.response.AdminAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAccountService {
    private final AdminAccountQueryRepository adminAccountQueryRepository;
    private final AdminRoleAssignmentRepository adminRoleAssignmentRepository;

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
}
