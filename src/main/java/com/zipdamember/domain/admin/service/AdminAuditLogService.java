package com.zipdamember.domain.admin.service;

import com.zipdamember.domain.admin.constant.AdminAuditResult;
import com.zipdamember.domain.admin.constant.AdminAuditActorType;
import com.zipdamember.domain.admin.entity.AdminAuditLog;
import com.zipdamember.domain.admin.entity.AdminAuditLogChange;
import com.zipdamember.domain.admin.repository.AdminAuditLogChangeRepository;
import com.zipdamember.domain.admin.repository.AdminAuditLogRepository;
import com.zipdamember.domain.admin.request.AdminAuditLogWriteRequest;
import com.zipdamember.global.context.TraceIdContext;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {
    private static final int TARGET_TYPE_MAX_LENGTH = 40;
    private static final int TARGET_ID_MAX_LENGTH = 100;
    private static final int REASON_MAX_LENGTH = 500;
    private static final int IP_ADDRESS_MAX_LENGTH = 45;
    private static final int USER_AGENT_MAX_LENGTH = 512;
    private static final int FIELD_NAME_MAX_LENGTH = 100;

    private static final Set<String> PROHIBITED_CHANGE_FIELDS = Set.of(
            "password",
            "adminpassword",
            "accesstoken",
            "refreshtoken",
            "token",
            "documentcontent",
            "filecontent",
            "signedurl",
            "documenturl"
    );

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final AdminAuditLogChangeRepository adminAuditLogChangeRepository;

    @Transactional
    public Long recordSuccess(AdminAuditLogWriteRequest request) {
        // 감사 로그 입력값 검증
        validate(request);

        // 감사 로그 원문 생성
        AdminAuditLog auditLog = AdminAuditLog.create(
                request.adminId(),
                request.actorType(),
                request.roleCode(),
                request.action(),
                request.targetService(),
                request.targetType().strip(),
                request.targetId().strip(),
                request.reason().strip(),
                normalizeOptionalValue(request.ipAddress(), IP_ADDRESS_MAX_LENGTH),
                normalizeOptionalValue(request.userAgent(), USER_AGENT_MAX_LENGTH),
                TraceIdContext.getOrCreate(),
                AdminAuditResult.SUCCESS
        );

        // 감사 로그 원문 저장
        AdminAuditLog savedAuditLog = adminAuditLogRepository.save(auditLog);

        // 감사 변경 상세 목록 생성
        List<AdminAuditLogChange> changes = request.changes().stream()
                .map(change -> AdminAuditLogChange.create(
                        savedAuditLog.getAuditLogId(),
                        change.fieldName().strip(),
                        change.beforeValue(),
                        change.afterValue(),
                        change.valueType(),
                        change.displayOrder()
                ))
                .toList();

        if (!changes.isEmpty()) {
            // 감사 변경 상세 저장
            adminAuditLogChangeRepository.saveAll(changes);
        }

        // 감사 로그 식별자 반환
        return savedAuditLog.getAuditLogId();
    }

    private void validate(AdminAuditLogWriteRequest request) {
        // 공통 필수값 검증
        if (request == null
                || request.actorType() == null
                || request.action() == null
                || request.targetService() == null) {
            throw invalidRequest("관리자 감사 로그 필수 값이 없습니다.");
        }

        // 관리자 행위자 식별 정보 검증
        if (request.actorType() == AdminAuditActorType.ADMIN) {
            if (request.adminId() == null || request.roleCode() == null) {
                throw invalidRequest("관리자 행위 감사 로그에는 관리자 아이디와 역할이 필요합니다.");
            }
        }

        // 감사 대상 및 요청 정보 검증
        validateRequiredValue(request.targetType(), TARGET_TYPE_MAX_LENGTH, "대상 유형");
        validateRequiredValue(request.targetId(), TARGET_ID_MAX_LENGTH, "대상 아이디");
        validateRequiredValue(request.reason(), REASON_MAX_LENGTH, "처리 사유");
        validateOptionalValue(request.ipAddress(), IP_ADDRESS_MAX_LENGTH, "IP 주소");
        validateOptionalValue(request.userAgent(), USER_AGENT_MAX_LENGTH, "User-Agent");
        validateChanges(request.changes());
    }

    private void validateChanges(List<AdminAuditLogWriteRequest.Change> changes) {
        Set<String> fieldNames = new HashSet<>();

        for (AdminAuditLogWriteRequest.Change change : changes) {
            // 변경 상세 필수값 검증
            if (change == null || change.valueType() == null || change.displayOrder() == null) {
                throw invalidRequest("감사 변경 상세 필수 값이 없습니다.");
            }

            // 변경 필드명 및 표시 순서 검증
            validateRequiredValue(change.fieldName(), FIELD_NAME_MAX_LENGTH, "변경 필드");
            if (change.displayOrder() < 0) {
                throw invalidRequest("감사 변경 상세 표시 순서는 0 이상이어야 합니다.");
            }

            String normalizedFieldName = normalizeFieldName(change.fieldName());

            // 변경 필드 중복 방지
            if (!fieldNames.add(normalizedFieldName)) {
                throw invalidRequest("동일한 감사 변경 필드를 중복 저장할 수 없습니다.");
            }

            // 민감정보 변경 상세 저장 방지
            if (PROHIBITED_CHANGE_FIELDS.contains(normalizedFieldName)) {
                throw invalidRequest("비밀번호, 토큰, 서류 원문은 감사 변경 상세에 저장할 수 없습니다.");
            }
        }
    }

    private void validateRequiredValue(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            throw invalidRequest(fieldName + "은(는) 필수입니다.");
        }

        validateOptionalValue(value, maxLength, fieldName);
    }

    private void validateOptionalValue(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw invalidRequest(fieldName + "은(는) " + maxLength + "자를 초과할 수 없습니다.");
        }
    }

    private String normalizeOptionalValue(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }

    private String normalizeFieldName(String fieldName) {
        return fieldName.strip()
                .replace("_", "")
                .replace("-", "")
                .toLowerCase(Locale.ROOT);
    }

    private BusinessException invalidRequest(String message) {
        return new BusinessException(CustomResponseCode.INVALID_PARAMETER_ERROR, message);
    }
}
