package com.zipdamember.domain.member.service;

import com.zipdamember.domain.admin.constant.AdminAuditAction;
import com.zipdamember.domain.admin.constant.AdminAuditActorType;
import com.zipdamember.domain.admin.constant.AdminAuditTargetService;
import com.zipdamember.domain.admin.constant.AdminAuditValueType;
import com.zipdamember.domain.admin.constant.AdminRoleCode;
import com.zipdamember.domain.admin.entity.AdminAuditLog;
import com.zipdamember.domain.admin.entity.AdminAuditLogChange;
import com.zipdamember.domain.admin.request.AdminAuditLogWriteRequest;
import com.zipdamember.domain.admin.repository.AdminAuditLogChangeRepository;
import com.zipdamember.domain.admin.repository.AdminAuditLogRepository;
import com.zipdamember.domain.admin.service.AdminAuditLogService;
import com.zipdamember.domain.auth.entity.LoginSession;
import com.zipdamember.domain.auth.repository.LoginSessionRepository;
import com.zipdamember.domain.member.constant.MemberSanctionScope;
import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.domain.member.entity.MemberAccount;
import com.zipdamember.domain.member.entity.MemberSanction;
import com.zipdamember.domain.member.repository.MemberAccountRepository;
import com.zipdamember.domain.member.repository.MemberSanctionHistorySource;
import com.zipdamember.domain.member.repository.MemberSanctionQueryRepository;
import com.zipdamember.domain.member.repository.MemberSanctionRepository;
import com.zipdamember.domain.member.request.AdminMemberSuspendReleaseRequest;
import com.zipdamember.domain.member.request.AdminMemberSuspendRequest;
import com.zipdamember.domain.member.response.AdminMemberSanctionHistoryResponse;
import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.error.custom.business.DuplicatedResourceException;
import com.zipdamember.global.error.custom.business.NotFoundResourceException;
import com.zipdamember.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
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
public class AdminMemberSanctionService {

    private final MemberAccountRepository memberAccountRepository;
    private final MemberSanctionRepository memberSanctionRepository;
    private final MemberSanctionQueryRepository memberSanctionQueryRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final AdminAuditLogService adminAuditLogService;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final AdminAuditLogChangeRepository adminAuditLogChangeRepository;

    @Transactional(readOnly = true)
    public AdminMemberSanctionHistoryResponse getHistories(Long memberId) {
        // 회원 제재·해제 이력 원본 조회
        List<MemberSanctionHistorySource> sources = memberSanctionQueryRepository
                .findHistorySourcesByMemberId(memberId);

        if (sources.isEmpty()) {
            return AdminMemberSanctionHistoryResponse.from(List.of());
        }

        // 제재 식별자 목록 생성
        List<String> sanctionIds = sources.stream()
                .map(source -> source.sanctionId().toString())
                .toList();

        // 제재·해제 감사 로그 조회
        List<AdminAuditLog> auditLogs = adminAuditLogRepository
                .findAllByTargetTypeAndTargetIdInOrderByOccurredAtDesc(
                        "MEMBER_SANCTION",
                        sanctionIds
                );

        // 제재 행위별 최신 감사 로그 구성
        Map<String, AdminAuditLog> auditLogByAction = indexAuditLogs(auditLogs);

        // 회원 상태 변경 상세 조회
        Map<Long, AdminAuditLogChange> statusChangeByAuditLogId = findStatusChanges(auditLogs);

        List<AdminMemberSanctionHistoryResponse.History> histories = new ArrayList<>();
        for (MemberSanctionHistorySource source : sources) {
            AdminAuditLog suspendAuditLog = auditLogByAction.get(
                    auditKey(source.sanctionId(), AdminAuditAction.MEMBER_SUSPEND)
            );
            histories.add(createSuspendHistory(
                    source,
                    suspendAuditLog,
                    findStatusChange(statusChangeByAuditLogId, suspendAuditLog)
            ));

            if (source.releasedAt() != null) {
                AdminAuditLog releaseAuditLog = auditLogByAction.get(
                        auditKey(source.sanctionId(), AdminAuditAction.MEMBER_SUSPEND_RELEASE)
                );
                histories.add(createReleaseHistory(
                        source,
                        releaseAuditLog,
                        findStatusChange(statusChangeByAuditLogId, releaseAuditLog)
                ));
            }
        }

        // 변경일시 최신순 정렬
        histories.sort(Comparator.comparing(
                AdminMemberSanctionHistoryResponse.History::changedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return AdminMemberSanctionHistoryResponse.from(histories);
    }

    @Transactional
    public void suspend(
            Long memberId,
            AdminMemberSuspendRequest request,
            Long operatorAdminId,
            AdminRoleCode operatorRole,
            String ipAddress,
            String userAgent
    ) {
        LocalDateTime now = LocalDateTime.now();

        // 제재 기간 검증
        validatePeriod(request, now);

        // 대상 회원 잠금 조회
        MemberAccount member = memberAccountRepository.findWithLockByMemberId(memberId)
                .orElseThrow(() -> new NotFoundResourceException("대상 회원을 찾을 수 없습니다."));

        // 활성 제재 중복 검증
        if (memberSanctionQueryRepository.existsActiveByMemberIdAndScope(memberId, request.scope(), now)) {
            throw new DuplicatedResourceException("동일 범위의 활성 회원 제재가 존재합니다.");
        }

        // 계정 제재 대상 상태 검증
        validateMemberStatus(member, request.scope());

        // 회원 제재 이력 저장
        MemberSanction sanction = memberSanctionRepository.save(MemberSanction.create(
                memberId,
                request.scope(),
                request.reasonCode(),
                request.relatedReportId(),
                request.startAt(),
                request.endAt(),
                operatorAdminId
        ));

        MemberStatus beforeStatus = member.getStatus();
        if (request.scope() == MemberSanctionScope.ACCOUNT) {
            // 회원 계정 정지 처리
            member.setStatus(MemberStatus.SUSPENDED);

            // 회원 활성 세션 폐기
            revokeActiveSessions(memberId);
        }

        // 회원 정지 감사 로그 저장
        saveAuditLog(
                sanction,
                request,
                operatorAdminId,
                operatorRole,
                beforeStatus,
                member.getStatus(),
                ipAddress,
                userAgent
        );
    }

    @Transactional
    public void release(
            Long sanctionId,
            AdminMemberSuspendReleaseRequest request,
            Long operatorAdminId,
            AdminRoleCode operatorRole,
            String ipAddress,
            String userAgent
    ) {
        LocalDateTime now = LocalDateTime.now();

        // 회원 제재 잠금 조회
        MemberSanction sanction = memberSanctionRepository.findWithLockBySanctionId(sanctionId)
                .orElseThrow(() -> new NotFoundResourceException("회원 제재 이력을 찾을 수 없습니다."));

        // 활성 제재 상태 검증
        if (!sanction.isActiveAt(now)) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "활성 상태의 회원 제재만 해제할 수 있습니다."
            );
        }

        // 회원 제재 해제 처리
        sanction.release(operatorAdminId, now);

        MemberStatus beforeStatus = null;
        MemberStatus afterStatus = null;
        if (sanction.getScope() == MemberSanctionScope.ACCOUNT) {
            // 계정 제재 해제 상태 복구
            MemberAccount member = memberAccountRepository.findWithLockByMemberId(sanction.getMemberId())
                    .orElse(null);
            if (member != null) {
                beforeStatus = member.getStatus();
                restoreMemberStatusIfAvailable(sanction, member, now);
                afterStatus = member.getStatus();
            }
        }

        // 회원 정지 해제 감사 로그 저장
        saveReleaseAuditLog(
                sanction,
                request,
                operatorAdminId,
                operatorRole,
                beforeStatus,
                afterStatus,
                ipAddress,
                userAgent
        );
    }

    private void validatePeriod(AdminMemberSuspendRequest request, LocalDateTime now) {
        // 제재 시작일시 검증
        if (request.startAt().isAfter(now)) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "제재 시작일시는 현재 이후로 지정할 수 없습니다."
            );
        }

        // 제재 종료일시 검증
        if (request.endAt() != null && !request.endAt().isAfter(now)) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "제재 종료일시는 현재 이후여야 합니다."
            );
        }
    }

    private Map<String, AdminAuditLog> indexAuditLogs(List<AdminAuditLog> auditLogs) {
        Map<String, AdminAuditLog> auditLogByAction = new HashMap<>();

        for (AdminAuditLog auditLog : auditLogs) {
            if (auditLog.getAction() != AdminAuditAction.MEMBER_SUSPEND
                    && auditLog.getAction() != AdminAuditAction.MEMBER_SUSPEND_RELEASE) {
                continue;
            }

            // 제재 행위별 최신 감사 로그 보관
            auditLogByAction.putIfAbsent(
                    auditLog.getTargetId() + ":" + auditLog.getAction().name(),
                    auditLog
            );
        }
        return auditLogByAction;
    }

    private Map<Long, AdminAuditLogChange> findStatusChanges(List<AdminAuditLog> auditLogs) {
        List<Long> auditLogIds = auditLogs.stream()
                .map(AdminAuditLog::getAuditLogId)
                .toList();

        if (auditLogIds.isEmpty()) {
            return Map.of();
        }

        // 회원 상태 변경 상세 조회
        return adminAuditLogChangeRepository
                .findAllByAuditLogIdInAndFieldName(auditLogIds, "memberStatus")
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        AdminAuditLogChange::getAuditLogId,
                        change -> change
                ));
    }

    private AdminMemberSanctionHistoryResponse.History createSuspendHistory(
            MemberSanctionHistorySource source,
            AdminAuditLog auditLog,
            AdminAuditLogChange statusChange
    ) {
        // 제재 이력 응답 생성
        return new AdminMemberSanctionHistoryResponse.History(
                source.sanctionId().toString(),
                "SANCTION",
                beforeStatus(source, statusChange, MemberStatus.ACTIVE),
                afterStatus(source, statusChange, MemberStatus.SUSPENDED),
                source.scope(),
                source.reasonCode(),
                auditLog == null ? source.reasonCode().name() : auditLog.getReason(),
                source.startAt(),
                source.endAt(),
                source.releasedAt(),
                source.sanctionedByName(),
                source.releasedByName(),
                auditLog == null ? null : auditLog.getRoleCode(),
                source.sanctionedByName(),
                auditLog == null ? source.startAt() : auditLog.getOccurredAt()
        );
    }

    private AdminMemberSanctionHistoryResponse.History createReleaseHistory(
            MemberSanctionHistorySource source,
            AdminAuditLog auditLog,
            AdminAuditLogChange statusChange
    ) {
        // 제재 해제 이력 응답 생성
        return new AdminMemberSanctionHistoryResponse.History(
                source.sanctionId().toString(),
                "RELEASE",
                beforeStatus(source, statusChange, null),
                afterStatus(source, statusChange, null),
                source.scope(),
                source.reasonCode(),
                auditLog == null ? null : auditLog.getReason(),
                source.startAt(),
                source.endAt(),
                source.releasedAt(),
                source.sanctionedByName(),
                source.releasedByName(),
                auditLog == null ? null : auditLog.getRoleCode(),
                source.releasedByName(),
                auditLog == null ? source.releasedAt() : auditLog.getOccurredAt()
        );
    }

    private MemberStatus beforeStatus(
            MemberSanctionHistorySource source,
            AdminAuditLogChange statusChange,
            MemberStatus fallback
    ) {
        // 제재 범위별 이전 상태 반환
        if (source.scope() != MemberSanctionScope.ACCOUNT) {
            return null;
        }
        return parseMemberStatus(statusChange == null ? null : statusChange.getBeforeValue(), fallback);
    }

    private MemberStatus afterStatus(
            MemberSanctionHistorySource source,
            AdminAuditLogChange statusChange,
            MemberStatus fallback
    ) {
        // 제재 범위별 변경 상태 반환
        if (source.scope() != MemberSanctionScope.ACCOUNT) {
            return null;
        }
        return parseMemberStatus(statusChange == null ? null : statusChange.getAfterValue(), fallback);
    }

    private MemberStatus parseMemberStatus(String value, MemberStatus fallback) {
        // 회원 상태 코드 변환
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return MemberStatus.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private AdminAuditLogChange findStatusChange(
            Map<Long, AdminAuditLogChange> statusChangeByAuditLogId,
            AdminAuditLog auditLog
    ) {
        // 감사 로그 상태 변경 상세 반환
        return auditLog == null ? null : statusChangeByAuditLogId.get(auditLog.getAuditLogId());
    }

    private String auditKey(Long sanctionId, AdminAuditAction action) {
        // 제재 감사 로그 키 생성
        return sanctionId + ":" + action.name();
    }

    private void validateMemberStatus(MemberAccount member, MemberSanctionScope scope) {
        // 계정 제재 대상 활성 상태 검증
        if (scope == MemberSanctionScope.ACCOUNT && member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_PARAMETER_ERROR,
                    "활성 상태의 회원만 계정 제재할 수 있습니다."
            );
        }
    }

    private void revokeActiveSessions(Long memberId) {
        // 회원 활성 세션 조회
        List<LoginSession> sessions = loginSessionRepository
                .findAllByMemberIdAndRevokedAtIsNull(memberId);

        // 회원 활성 세션 폐기
        sessions.forEach(LoginSession::revoke);
    }

    private void restoreMemberStatusIfAvailable(
            MemberSanction sanction,
            MemberAccount member,
            LocalDateTime now
    ) {
        // 다른 활성 계정 제재 확인
        boolean hasOtherActiveSanction = memberSanctionQueryRepository
                .existsOtherActiveByMemberIdAndScope(
                        sanction.getMemberId(),
                        MemberSanctionScope.ACCOUNT,
                        sanction.getSanctionId(),
                        now
                );

        // 회원 활성 상태 복구
        if (!hasOtherActiveSanction && member.getStatus() == MemberStatus.SUSPENDED) {
            member.setStatus(MemberStatus.ACTIVE);
        }
    }

    private void saveAuditLog(
            MemberSanction sanction,
            AdminMemberSuspendRequest request,
            Long operatorAdminId,
            AdminRoleCode operatorRole,
            MemberStatus beforeStatus,
            MemberStatus afterStatus,
            String ipAddress,
            String userAgent
    ) {
        List<AdminAuditLogWriteRequest.Change> changes = new ArrayList<>();
        changes.add(new AdminAuditLogWriteRequest.Change(
                "memberId",
                null,
                sanction.getMemberId().toString(),
                AdminAuditValueType.NUMBER,
                0
        ));
        changes.add(new AdminAuditLogWriteRequest.Change(
                "scope",
                null,
                request.scope().name(),
                AdminAuditValueType.ENUM,
                1
        ));
        changes.add(new AdminAuditLogWriteRequest.Change(
                "reasonCode",
                null,
                request.reasonCode().name(),
                AdminAuditValueType.ENUM,
                2
        ));
        changes.add(new AdminAuditLogWriteRequest.Change(
                "startAt",
                null,
                request.startAt().toString(),
                AdminAuditValueType.DATETIME,
                3
        ));
        changes.add(new AdminAuditLogWriteRequest.Change(
                "endAt",
                null,
                request.endAt() == null ? null : request.endAt().toString(),
                AdminAuditValueType.DATETIME,
                4
        ));

        if (request.scope() == MemberSanctionScope.ACCOUNT) {
            changes.add(new AdminAuditLogWriteRequest.Change(
                    "memberStatus",
                    beforeStatus.name(),
                    afterStatus.name(),
                    AdminAuditValueType.ENUM,
                    5
            ));
        }

        // 회원 정지 감사 로그 저장
        adminAuditLogService.recordSuccess(new AdminAuditLogWriteRequest(
                operatorAdminId,
                AdminAuditActorType.ADMIN,
                operatorRole,
                AdminAuditAction.MEMBER_SUSPEND,
                AdminAuditTargetService.MEMBER,
                "MEMBER_SANCTION",
                sanction.getSanctionId().toString(),
                request.reasonCode().name(),
                ipAddress,
                userAgent,
                changes
        ));
    }

    private void saveReleaseAuditLog(
            MemberSanction sanction,
            AdminMemberSuspendReleaseRequest request,
            Long operatorAdminId,
            AdminRoleCode operatorRole,
            MemberStatus beforeStatus,
            MemberStatus afterStatus,
            String ipAddress,
            String userAgent
    ) {
        List<AdminAuditLogWriteRequest.Change> changes = new ArrayList<>();
        changes.add(new AdminAuditLogWriteRequest.Change(
                "memberId",
                null,
                sanction.getMemberId().toString(),
                AdminAuditValueType.NUMBER,
                0
        ));
        changes.add(new AdminAuditLogWriteRequest.Change(
                "releasedAt",
                null,
                sanction.getReleasedAt().toString(),
                AdminAuditValueType.DATETIME,
                1
        ));
        changes.add(new AdminAuditLogWriteRequest.Change(
                "adminReleasedBy",
                null,
                sanction.getAdminReleasedBy().toString(),
                AdminAuditValueType.NUMBER,
                2
        ));

        if (beforeStatus != null && afterStatus != null && beforeStatus != afterStatus) {
            changes.add(new AdminAuditLogWriteRequest.Change(
                    "memberStatus",
                    beforeStatus.name(),
                    afterStatus.name(),
                    AdminAuditValueType.ENUM,
                    3
            ));
        }

        // 회원 정지 해제 감사 로그 저장
        adminAuditLogService.recordSuccess(new AdminAuditLogWriteRequest(
                operatorAdminId,
                AdminAuditActorType.ADMIN,
                operatorRole,
                AdminAuditAction.MEMBER_SUSPEND_RELEASE,
                AdminAuditTargetService.MEMBER,
                "MEMBER_SANCTION",
                sanction.getSanctionId().toString(),
                request.releaseReason().strip(),
                ipAddress,
                userAgent,
                changes
        ));
    }
}
